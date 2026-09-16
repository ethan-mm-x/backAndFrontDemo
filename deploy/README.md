# 服务器部署包（无源码）

本目录给 **load 镜像后启动** 用，不含 Java 源码。

| 文件 | 说明 |
| --- | --- |
| `demo-images.tar` | 本机 `./scripts/pack-images.sh` 生成，勿提交 Git |
| `docker-compose.yml` | 只有 `image:`，无 `build:` |
| `.env.example` | 复制为 `.env` 填 AK/SK 与库密码 |
| `load-and-up.sh` | `docker load` + `compose up` |

详见仓库根目录 README 第 6 节。
