# Spring Cloud Lab

基于 Spring Boot 3.2.5 + Spring Cloud 的分布式商城学习项目，用于实践微服务架构与分布式组件。

## 技术栈

- **Spring Boot** 3.2.5 / **Spring Cloud** 2023.0.1 / **Spring Cloud Alibaba** 2023.0.1.0
- **Java** 21 / **MyBatis-Plus** 3.5.7 / **Lombok**
- **Nacos** 注册中心 / **OpenFeign** 服务间调用 / **Spring Cloud Gateway** 网关
- **Sa-Token** 1.38.0 登录认证（Redis 会话共享）
- **MySQL** 8.0（三库三账号）/ **Redis** 7
- **支付宝沙箱**（alipay-sdk-java：电脑网站支付 + 异步回调 + 轮询查单双保险）
- **XXL-Job** 2.4.1（订单超时关单）
- **Seata** 2.0.0（AT 分布式事务：下单跨库扣库存，全局提交/回滚实战验证）
- **RocketMQ** 5.3.1（延迟消息关单，XXL-Job 扫表退为兑底）
- 前端：**Vue 3 + TypeScript + Vite + Pinia**
- **Docker Compose** 基础设施编排

## 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | 123456 | 管理员 |
| test01 | 123456 | 测试用户 |
| test02 | 123456 | 测试用户 |

> 密码使用 BCrypt 加密存储，数据库中为密文。

## 模块说明

| 模块 | 端口 | 数据库 | 职责 |
|------|------|--------|------|
| lab-gateway | 8088 | - | 网关：路由转发、统一鉴权 |
| lab-user | 8083 | cloud_user | 注册/登录、用户管理 |
| lab-product | 8081 | cloud_product | 商品 CRUD、库存原子扣减 |
| lab-order | 8082 | cloud_order | 购物车、下单、支付宝支付、超时关单 |
| lab-common | - | - | Result/PageResult、全局异常、MyBatis-Plus 公共配置 |
| lab-frontend | 5174 | - | Vue3 前端（dev 代理 /api → 8088） |

## 基础设施（docker compose）

| 容器 | 宿主端口 | 说明 |
|------|----------|------|
| cloud-mall-mysql | 3307 | 三库三账号由 init.sql 自动初始化 |
| cloud-mall-redis | 6380 | Sa-Token 会话 |
| cloud-mall-nacos | 8848 | 注册中心（控制台 nacos/nacos） |
| cloud-xxljob-admin | 8181 | 任务调度中心（admin/123456） |
| cloud-seata-server | 7091 / 8091 | Seata TC（控制台 seata/seata，8091 客户端通信） |
| cloud-rmq-namesrv | 19876 | RocketMQ NameServer（默认 9876 被另一项目占用，端口后移） |
| cloud-rmq-broker | 10913/10915/10916 | RocketMQ Broker（brokerIP1 通告 127.0.0.1，详见 rocketmq/broker.conf 注释） |
| cloud-rmq-dashboard | 8182 | RocketMQ 控制台（8180/8181 已被占用） |

> xxl_job 库不在 init.sql 内：首次启动前需以 root 手动导入 `docker/tables_xxl_job.sql`。

## 数据库账号

| 账号 | 密码 | 权限 |
|------|------|------|
| cloud_user / cloud_product / cloud_order | cloud123 | 账号=库名，仅自己的库全部权限 |
| cloudlab | cloud123 | 三业务库 + xxl_job 只读（人工查看用） |
| root | root123 | 全部 |

## 快速开始

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d
```

- MySQL 数据目录为空时自动执行 init.sql（建三库三账号 + 表 + 种子数据）；xxl_job 库需手动导入
- 等约 30 秒 Nacos 就绪后再启动 Spring 应用

### 2. 启动后端服务

在 IDEA 中依次运行 `LabGatewayApplication` / `LabUserApplication` / `LabProductApplication` / `LabOrderApplication`。

**业务服务与网关之间没有启动顺序要求**：路由和 Feign 都走 Nacos 按服务名动态解析，被调方未注册时请求临时报错（503 / 无可用实例），注册后自动恢复。

### 3. 启动前端

```bash
cd lab-frontend
npm install
npm run dev
```

访问 http://localhost:5174

### 4. 支付宝沙箱前置（可选）

- 复制 `lab-order/src/main/resources/application-local.yml.example` 为 `application-local.yml`，填入沙箱 APPID 与密钥（不入版本库）
- 异步回调需公网：natapp 隧道映射 8088，地址同步到 notify-url；无回调时轮询查单兜底仍可闭环

## 核心业务链路

登录（Sa-Token）→ 商品 → 购物车 → 下单（Feign 扣库存 + 发 RocketMQ 延迟关单消息）→ 支付宝沙箱支付（回调 + 轮询双保险）→ 超时关单（MQ 延迟消息到点为主、XXL-Job 扫表兑底；渠道关单 + 库存回补）

> 三库拆分后，跨服务一致性由 Seata AT 保证（undo_log 三库各一张；下单链路已实战验证全局回滚）。

## 项目规划

1. ~~基础搭建：MySQL + 用户模块 CRUD~~ ✅
2. ~~模块拆分：用户 / 商品 / 订单 / 网关 / 前端~~ ✅
3. ~~进阶组件：Nacos、Gateway、Sa-Token、支付宝沙箱、XXL-Job 超时关单~~ ✅
4. ~~Seata 分布式事务：下单链路 @GlobalTransactional + undo_log 反向补偿~~ ✅
5. ~~RocketMQ 延迟消息关单（XXL-Job 退为兑底）~~ ✅
6. Sentinel 限流熔断
