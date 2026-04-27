# 部署指南

此文件夹包含一套用于你的微服务项目的基础部署方案：

* Docker 本地集群：`deploy/docker/docker-compose.yml`
* Kubernetes 清单文件：`deploy/k8s/*.yaml`
* 通用 Java 服务镜像构建 Dockerfile：`deploy/docker/java-service.Dockerfile`

## 1）本地容器集群（Docker Compose）

1. 复制环境变量模板：

   * `cp deploy/docker/.env.example deploy/docker/.env`
2. 在 `deploy/docker/.env` 中填写真实密钥和配置。
3. 启动服务：

   * `docker compose --env-file deploy/docker/.env -f deploy/docker/docker-compose.yml up -d --build`
4. 访问网关：

   * `http://127.0.0.1:19080`

## 2）Kubernetes 集群部署

1. 创建命名空间和配置：

   * `kubectl apply -f deploy/k8s/namespace.yaml`
   * `kubectl apply -f deploy/k8s/configmap.yaml`
2. 基于模板创建 Secret：

   * `cp deploy/k8s/secret.example.yaml deploy/k8s/secret.yaml`
   * 编辑 `secret.yaml`，填入真实值
   * `kubectl apply -f deploy/k8s/secret.yaml`
3. 部署中间件和服务：

   * `kubectl apply -f deploy/k8s/mysql.yaml`
   * `kubectl apply -f deploy/k8s/redis.yaml`
   * `kubectl apply -f deploy/k8s/apps.yaml`
4. 可选：部署 Ingress

   * `kubectl apply -f deploy/k8s/ingress.yaml`

## 3）说明

* `skin-gateway` 现在支持通过环境变量覆盖路由 URI：

  * `AUTH_SERVICE_URI`
  * `AI_SERVICE_URI`
  * `CORE_SERVICE_URI`
  * `LSATRANS_SERVICE_URI`
* 当前的数据库和 Redis 使用的是单实例模板。如果要用于生产环境高可用，建议切换为托管版 MySQL/Redis，或使用带持久化和备份能力的 Helm Charts。
* 不要提交已经填入真实密钥的 Secret 文件。

***

**注意**

- 需要把 `deploy/k8s/apps.yaml` 里的镜像名 `ghcr.io/your-org/...` 改成真实的组织/仓库。
- `wechat-upload.yml` 需要先在 GitHub Secrets 配置：`WECHAT_APPID`、`WECHAT_PRIVATE_KEY`、`WECHAT_CI_ROBOT`。
