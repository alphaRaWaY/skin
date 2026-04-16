# Skin - AI 皮肤分析系统

<p align="center">
  <img src="skin-front/src/static/images/logo_icon.jpg" alt="Skin Logo" width="96" />
</p>

<p align="center">
  前后端分离的皮肤分析项目，支持图片上传、分析结果查看、家庭成员管理、历史记录与聊天能力。
</p>

<p align="center">
  <img src="https://img.shields.io/badge/node.js-20.x-5FA04E?logo=nodedotjs" alt="node.js-20.x" />
  <img src="https://img.shields.io/badge/Java-17-red?logo=java&logoColor=white" alt="Java-17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?logo=springboot&logoColor=white" alt="Spring_Boot-3.4.3" />
</p>

## 项目结构

```text
skin/
├─ skin_front/       # Uni-app + Vue3 + TypeScript 前端
├─ skin_back/        # Spring Boot + MyBatis 后端
├─ skin_LSAtrans/    # 核心算法
└─ README.md
```

前端核心目录：
- `skin_front/src/pages` 页面
- `skin_front/src/services` 接口封装
- `skin_front/src/stores` 状态管理

后端核心目录：
- `skin_back/src/main/java/org/crythic` 业务代码
- `skin_back/src/main/resources/data.sql` 初始化 SQL

## 技术栈

- 前端：`uni-app`、`Vue 3`、`TypeScript`、`Pinia`
- 后端：`Spring Boot 3.4.3`、`MyBatis`、`MySQL`、`Redis`
- 其他：`JWT`、`Spring AI`、阿里云 OSS

## 环境要求

- Node.js `20.x`（建议配合 `pnpm`）
- JDK `17`
- Maven（或使用仓库内 `mvnw/mvnw.cmd`）
- MySQL、Redis（按本地配置准备）

## 快速开始

### 1) 启动后端

```bash
cd skin_back
# Windows
mvnw.cmd spring-boot:run
# macOS/Linux
./mvnw spring-boot:run
```

### 2) 启动前端

```bash
cd skin_front
pnpm install
pnpm dev:h5
```

默认前端请求地址在 `skin_front/src/utils/http.ts` 中配置为 `http://localhost:8080/api`。

## 常用命令

前端：
- `pnpm dev:h5` 启动 H5 开发环境
- `pnpm dev:mp-weixin` 启动微信小程序开发环境
- `pnpm build:h5` 打包 H5
- `pnpm lint` 代码检查并自动修复
- `pnpm tsc` TypeScript 类型检查

后端：
- `mvnw.cmd test` 运行测试
- `mvnw.cmd clean package` 打包 Jar

## 配置说明

后端配置文件被 `.gitignore` 忽略，需要本地创建：
- `skin_back/src/main/resources/application.yml`（或 `application.properties`）

建议至少配置：
- `spring.datasource.*`（MySQL 连接）
- `spring.data.redis.*`（Redis 连接）
- JWT 密钥、OSS 参数、第三方 AI 接口参数

## 说明

- 当前仓库只维护最外层 `README.md`。
- 子项目 `README.md` 已删除，不再作为文档入口。
