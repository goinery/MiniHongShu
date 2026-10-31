import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { setImmediate } from "node:timers";
import vm from "node:vm";
import { build } from "esbuild";

// 运行实际的 store 和 Axios 拦截器，只替换 UI 提示和网络传输。
const root = fileURLToPath(new URL("../", import.meta.url));
const bundle = await build({
  absWorkingDir: root,
  stdin: {
    contents: `export { default as request } from "./src/utils/request";
      export { useUserStore } from "./src/store/userStore";
      export { storage } from "./src/utils/storage";`,
    resolveDir: root,
    loader: "ts",
  },
  bundle: true,
  write: false,
  platform: "node",
  format: "cjs",
  packages: "external",
  alias: { "@": `${root}src` },
  define: { "import.meta.env.VITE_APP_BASE_API": '"/prod-api"' },
  plugins: [{
    name: "message-spy",
    setup(builder) {
      builder.onResolve({ filter: /^element-plus$/ }, () => ({ path: "messages", namespace: "test" }));
      builder.onLoad({ filter: /.*/, namespace: "test" }, () => ({
        contents: "export const ElMessage = globalThis.messages;",
      }));
    },
  }],
});

/** @typedef {{ request: typeof import("../src/utils/request").default, useUserStore: typeof import("../src/store/userStore").useUserStore, storage: typeof import("../src/utils/storage").storage }} SessionModule */

function createUser() {
  return {
    id: "42", yxId: "42", username: "test-user", avatar: "", gender: 0,
    description: "", birthday: "", address: "",
    trendCount: 0, followerCount: 0, fanCount: 0,
  };
}

let request, user, storage, warnings, errors, logoutCount;
beforeEach(() => {
  warnings = [];
  errors = [];
  logoutCount = 0;
  const entries = new Map();
  const window = new EventTarget();
  window.addEventListener("hongshu:logout", () => logoutCount++);
  const sandbox = {
    module: { exports: {} },
    require: createRequire(import.meta.url),
    window,
    Event,
    FormData,
    localStorage: {
      getItem: (key) => entries.get(key) ?? null,
      setItem: (key, value) => entries.set(key, value),
      removeItem: (key) => entries.delete(key),
    },
    messages: { warning: (message) => warnings.push(message), error: (message) => errors.push(message) },
  };
  vm.runInNewContext(bundle.outputFiles[0].text, sandbox);
  const sessionModule = /** @type {SessionModule} */ (sandbox.module.exports);
  ({ request, storage } = sessionModule);
  user = sessionModule.useUserStore();
  storage.set("accessToken", "old-access");
  storage.set("refreshToken", "old-refresh");
  storage.set("userInfo", { id: "42", username: "cached-user" });
});

function respond(config, data) {
  return { config, data, status: 200, statusText: "OK", headers: {} };
}

function assertLoggedOut() {
  assert.equal(user.isLogin(), false);
  for (const key of ["accessToken", "refreshToken", "userInfo"]) assert.equal(storage.get(key), undefined);
  assert.equal(user.getUserInfo(), null);
}

test("启动时不信任缓存资料，失效会话自动清理", async () => {
  assert.equal(user.isLogin(), false);
  request.defaults.adapter = async (config) => respond(config, { code: 401, message: "token异常" });
  await user.restoreSession();
  assertLoggedOut();
  assert.equal(logoutCount, 1);
  assert.equal(warnings.length, 1);
});

test("有效会话通过服务端校验后恢复，重复导航不重复校验", async () => {
  let calls = 0;
  request.defaults.adapter = async (config) => {
    calls++;
    assert.equal(config.headers.accessToken, "old-access");
    assert.equal(config.params, undefined);
    return respond(config, { code: 200, data: { id: "42", username: "server-user" } });
  };
  await Promise.all([user.restoreSession(), user.restoreSession()]);
  await user.restoreSession();
  assert.equal(user.isLogin(), true);
  assert.equal(user.getUserInfo().username, "server-user");
  assert.equal(calls, 1);
});

test("仅有用户资料和刷新凭证的残留也会清理", async () => {
  storage.remove("accessToken");
  await user.restoreSession();
  assertLoggedOut();
});

test("并发过期请求只刷新一次，并使用新 token 重试", async () => {
  user.setUserInfo(createUser());
  let refreshCalls = 0;
  let retries = 0;
  request.defaults.adapter = async (config) => {
    if (config.url.includes("refreshToken")) {
      refreshCalls++;
      await new Promise((resolve) => setImmediate(resolve));
      return respond(config, { code: 200, data: { accessToken: "new-access", refreshToken: "old-refresh" } });
    }
    if (config.headers.accessToken === "old-access") return respond(config, { code: 501 });
    retries++;
    assert.equal(config.headers.accessToken, "new-access");
    return respond(config, { code: 200 });
  };
  await Promise.all([request.get("/protected/a"), request.get("/protected/b")]);
  assert.equal(refreshCalls, 1);
  assert.equal(retries, 2);
  assert.equal(user.isLogin(), true);
  assert.equal(logoutCount, 0);
});

test("刷新凭证失效时清空登录态，并发错误只提示一次", async () => {
  user.setUserInfo(createUser());
  request.defaults.adapter = async (config) => respond(config, { code: config.url.includes("refreshToken") ? 401 : 501 });
  await Promise.allSettled([request.get("/protected/a"), request.get("/protected/b")]);
  assertLoggedOut();
  assert.equal(warnings.length, 1);
  assert.equal(logoutCount, 1);
});

test("过期且缺少刷新凭证时退出", async () => {
  user.setUserInfo(createUser());
  storage.remove("refreshToken");
  request.defaults.adapter = async (config) => respond(config, { code: 501 });
  await assert.rejects(request.get("/protected"));
  assertLoggedOut();
});

test("网络故障保留凭证，恢复网络后可以重新验证", async () => {
  request.defaults.adapter = async () => { throw new Error("network unavailable"); };
  await user.restoreSession();
  assert.equal(user.isLogin(), false);
  assert.equal(storage.get("accessToken"), "old-access");
  assert.equal(logoutCount, 0);
  request.defaults.adapter = async (config) => respond(config, { code: 200, data: { id: "42" } });
  await user.restoreSession();
  assert.equal(user.isLogin(), true);
});

test("HTTP 401 同样清理会话，普通业务 500 不清理", async () => {
  user.setUserInfo(createUser());
  request.defaults.adapter = async (config) => respond(config, { code: 500, message: "temporary failure" });
  await assert.rejects(request.get("/protected"));
  assert.equal(user.isLogin(), true);
  request.defaults.adapter = async () => { throw { response: { status: 401 } }; };
  await assert.rejects(request.get("/protected"));
  assertLoggedOut();
});

test("退出后晚到的刷新响应不能恢复旧会话", async () => {
  user.setUserInfo(createUser());
  request.defaults.adapter = async (config) => {
    if (config.url.includes("refreshToken")) {
      user.loginOut();
      return respond(config, { code: 200, data: { accessToken: "new-access", refreshToken: "old-refresh" } });
    }
    return respond(config, { code: 501 });
  };
  await assert.rejects(request.get("/protected"));
  assertLoggedOut();
});
