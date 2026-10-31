# 本机工具部署

Java、Maven、Node.js 均使用本机安装的版本：Maven 在本机打包后端，Java 在本机运行后端，Node.js 在本机编译前端。Docker Desktop 使用 Linux 容器，仅运行 MySQL、Redis 和前端 Nginx，不构建应用镜像。

以下命令均在项目根目录（`compose.yaml` 所在目录）的 PowerShell 中执行。

## 本机环境

安装 JDK 21、Maven、Node.js，将 `java`、`mvn`、`node`、`npm` 加入 PATH。Maven 使用 `JAVA_HOME` 选择 JDK，必须指向 JDK 21。

例如，本机 JDK 21 位于 `D:\Java\jdk-21`：

```powershell
# 修改当前用户的默认 JDK，新开的终端会生效
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'D:\Java\jdk-21', 'User')

# 让当前 PowerShell 立即生效
$env:JAVA_HOME = 'D:\Java\jdk-21'

# 查看 Maven 实际使用的 Java，输出应为 21
mvn -version
```

修改用户环境变量后，已经打开的终端、IDE 和 Codex 可能仍保留旧值；可重启这些程序，或在当前终端设置 `$env:JAVA_HOME`。部署脚本也支持 `-JavaHome 'D:\Java\jdk-21'`，只对本次运行生效。

## 启动和更新

已有 `.env` 就继续使用。新环境先复制配置，再填写密码、JWT 密钥和 OSS 配置：

```powershell
Copy-Item .env.example .env
```

启动 Docker Desktop 后，构建并部署全部应用：

```powershell
./deploy/local.ps1
```

脚本会使用本机 Maven 打包后端（跳过测试），使用本机 npm 编译两个前端；前端缺少依赖时自动执行 `npm ci`，已有依赖则直接构建。修改 `package-lock.json` 后，在对应前端目录执行 `npm ci --cache ../../.cache/npm`，再部署。

MySQL、Redis 就绪后，脚本在后台启动本机 Java，并启动前端 Nginx。Nginx 挂载本机 `dist` 目录，通过 `host.docker.internal` 访问本机后端。

| 服务 | 本机访问地址 | 运行方式 |
| --- | --- | --- |
| 用户前台 | http://localhost:8081 | 本机编译，Docker Nginx 提供页面 |
| 管理后台 | http://localhost:8082 | 本机编译，Docker Nginx 提供页面 |
| 后端接口 | http://localhost:8080 | 本机 Java |
| MySQL | localhost:3306 | Docker |
| Redis | localhost:6379 | Docker |

前台、管理后台和后端端口可在 `.env` 中修改。管理员初始账号密码也在 `.env` 中配置，仅首次初始化时生效。为供 Docker Nginx 访问，本机后端监听 `0.0.0.0`。

只更新两个前端（后端已在运行）：

```powershell
./deploy/local.ps1 -Target Frontend
```

只更新用户前台、管理后台或后端，按需执行其中一条：

```powershell
./deploy/local.ps1 -Target Web
./deploy/local.ps1 -Target Admin
./deploy/local.ps1 -Target Backend
```

修改 `.env` 的 `APP_BRAND_NAME` 后，使用 `-Target Frontend` 重新编译两个前端。修改后端密码、密钥或 OSS 配置后，使用 `-Target Backend` 重新部署后端。

已完成本机构建，只需启动或重新部署现有产物：

```powershell
./deploy/local.ps1 -SkipBuild
```

脚本优先使用本地 `nginx:1.28-alpine`；也可自动复用已有 `hongshu-web:local` 或 `hongshu-admin:local` 镜像中的 Nginx，页面以挂载的本机产物为准。新环境没有可用镜像时才下载 Nginx。可在 `.env` 中设置 `NGINX_IMAGE` 指定镜像。MySQL、Redis 同样优先使用本地镜像，缺少时下载。

从旧 Docker 部署迁移时，脚本会停止旧后端容器，再启动本机 Java；保留原有 MySQL、Redis 数据卷。原来的后端容器不再由 `compose.yaml` 管理。

## 日常命令

```powershell
# 查看 Docker 容器与本机后端状态
./deploy/local.ps1 -Action Status

# 查看本机后端日志；Ctrl+C 只退出日志查看
Get-Content .cache/logs/backend/stdout.log -Tail 100 -Wait

# 查看本机后端错误输出
Get-Content .cache/logs/backend/stderr.log -Tail 100

# 查看 Nginx 日志
docker compose logs -f --tail=100 web admin

# 停止本机后端和全部容器，保留数据
./deploy/local.ps1 -Action Stop

# 只停止本机后端
./deploy/local.ps1 -Action Stop -Target Backend
```

电脑重启后，运行 `./deploy/local.ps1 -SkipBuild` 启动本机后端及现有前端产物。`docker compose restart` 只影响 Docker 容器；本机后端的启动、更新和停止使用上述脚本。

## 数据和临时文件

MySQL 和 Redis 分别保存在 `hongshu_mysql-data`、`hongshu_redis-data` 数据卷。旧本地上传文件在 `HongShu-backend/upload`，新上传文件使用 `.env` 配置的 OSS。

运行中的后端 JAR 和进程记录放在 `.cache/backend`，控制台日志放在 `.cache/logs/backend`，后端文件日志放在 `.cache/backend/logs`。Maven/npm 缓存及临时文件均放在项目内 `.cache`，该目录已排除 Git。构建产物保存在各模块原有的 `target`、`dist` 目录。

空数据库首次启动会自动执行 `HongShu-backend/sql/hongshu_mock.sql`，创建表和演示数据；已有数据库不会重复初始化。日常停止使用脚本的 `Stop` 操作，不要执行 `docker compose down -v`，否则会删除数据卷。
