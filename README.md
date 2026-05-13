# SkinAI Monorepo

<p align="center">
  <img src="skin-front/src/static/images/logo_icon.jpg" alt="SkinAI Logo" width="96" />
</p>

本仓库是一个多模块项目，包含后端 Java 服务、前端应用和 Python 算法模块。

## 目录结构

```text
skin/
├─ skin-common/      # 后端公共能力（工具、校验、注解）
├─ skin-pojo/        # 后端模型层（DTO/VO/Entity）
├─ skin-server/      # 后端业务服务（当前主服务）
├─ skin-gateway/     # 网关骨架（后续扩展为网关服务）
├─ skin-front/       # 前端项目（独立维护）
├─ skin-LSAtrans/    # Python 算法模块（独立维护）
├─ pom.xml           # Maven 父工程（聚合模块）
└─ README.md
```

## 技术栈

- Java 17, Spring Boot 3.4.x, Maven
- MyBatis, MySQL, Redis
- Spring AI, Aliyun OSS
- Uni-app + Vue3 (front)
- Python (LSAtrans)

## 后端构建与运行

在仓库根目录执行：

```bash
# 编译后端主服务及依赖模块
mvnw.cmd -pl skin-server -am -DskipTests compile

# 本地运行（使用 local 配置）
mvnw.cmd -pl skin-server -am spring-boot:run -Dspring-boot.run.profiles=local
```

说明：`skin-server` 默认激活 profile 为 `local`，也可通过 `SPRING_PROFILES_ACTIVE` 覆盖。

## 配置文件分层（环境隔离）

后端配置位于 `skin-server/src/main/resources`：

- `application.yml`：统一基础配置（入库）
- `application-example.yml`：示例配置模板（入库，开源）
- `application-local.yml`：本地开发配置（不入库）
- `application-dev.yml`：容器/部署环境配置（不入库）

当前 `.gitignore` 已忽略 `application-local.yml` 和 `application-dev.yml`。

## 前端与算法模块

- `skin-front`：前端独立演进，不与本次后端解耦绑定。
- `skin-LSAtrans`：算法模块独立演进，可通过接口与 `skin-server` 对接。

## 后续建议

- 将 `skin-gateway` 升级为 Spring Cloud Gateway。
- 按业务域继续拆分 `skin-server`（如 report/chat/user 等）为独立服务。
- 增加 Dockerfile 与 compose，实现本地一键联调。

## 部署

此仓库现已包含 
部署框架：

- Docker 本地集群: `deploy/docker/docker-compose.yml`
- Kubernetes 清单: `deploy/k8s/*.yaml`
- 部署指南: `deploy/README.md`
- CI/CD workflows:
  - `.github/workflows/ci.yml`
  - `.github/workflows/docker-publish.yml`
  - `.github/workflows/wechat-upload.yml`

## Doctor V2 Refactor Assets
- DB schema draft: deploy/sql/doctor_schema_v2.sql`r
- Backend API draft: deploy/docs/backend-api-v2.md`r

