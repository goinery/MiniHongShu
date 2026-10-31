import assert from "node:assert/strict";
import { test } from "node:test";
import vm from "node:vm";
import { fileURLToPath } from "node:url";
import { build } from "esbuild";

const bundle = await build({
  entryPoints: [fileURLToPath(new URL("../src/utils/noteImages.ts", import.meta.url))],
  bundle: true, write: false, platform: "node", format: "cjs",
});
const sandbox = { module: { exports: {} }, URL };
vm.runInNewContext(bundle.outputFiles[0].text, sandbox);
const { normalizeImageUrl, resolveNoteImages } = sandbox.module.exports;

test("图片链接支持无扩展名和 OSS 查询参数，拒绝非 HTTP 地址及账号密码", () => {
  const url = "https://images.example.com/asset?id=1&x-oss-process=image/resize,w_600";
  assert.equal(normalizeImageUrl(`  ${url}  `), url);
  for (const value of ["/relative.jpg", "javascript:alert(1)", "data:image/png;base64,abc", "file:///image.jpg", "https://user:secret@example.com/a.jpg", "http://localhost/a.jpg"]) {
    assert.throws(() => normalizeImageUrl(value));
  }
});

test("本地图片与链接混用保留顺序和首图，不下载外链或重新上传编辑图片", async () => {
  const first = { name: "first.png" };
  const last = { name: "last.png" };
  const files = [
    { name: "first", raw: first },
    { name: "remote", url: "https://images.example.com/remote.webp" },
    { name: "existing", url: "/profile/old.jpg" },
    { name: "last", raw: last },
  ];
  const calls = [];
  const urls = await resolveNoteImages(files, async (file) => {
    calls.push(file);
    return `/profile/${file.name}`;
  });
  assert.deepEqual(calls, [first, last]);
  assert.deepEqual(Array.from(urls), ["/profile/first.png", "https://images.example.com/remote.webp", "/profile/old.jpg", "/profile/last.png"]);
  assert.ok(files.every(file => !file.raw));
  await resolveNoteImages(files, () => assert.fail("重试不应再次上传"));
});

test("部分上传失败时保留已完成地址，重试只上传失败文件", async () => {
  const files = [{ name: "a", raw: { name: "a.png" } }, { name: "b", raw: { name: "b.png" } }];
  await assert.rejects(resolveNoteImages(files, async (file) => {
    if (file.name === "b.png") throw new Error("网络错误");
    return "/profile/a.png";
  }), /网络错误/);
  const calls = [];
  const result = await resolveNoteImages(files, async (file) => {
    calls.push(file.name);
    return "/profile/b.png";
  });
  assert.deepEqual(calls, ["b.png"]);
  assert.deepEqual(Array.from(result), ["/profile/a.png", "/profile/b.png"]);
});

test("纯链接无需上传，空列表及超过九张在上传前被拒绝", async () => {
  const upload = () => assert.fail("不应上传文件");
  const files = [{ name: "remote", url: "https://images.example.com/a.jpg" }];
  assert.deepEqual(Array.from(await resolveNoteImages(files, upload)), [files[0].url]);
  await assert.rejects(resolveNoteImages([], upload), /1～9/);
  await assert.rejects(resolveNoteImages(Array(10).fill(files[0]), upload), /1～9/);
});
