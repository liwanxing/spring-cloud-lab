# Spring Cloud Lab

基于 Spring Boot 3.2.5 + Spring Cloud 的分布式商城学习项目，用于实践微服务架构与分布式组件。

## 技术栈

- **Spring Boot** 3.2.5
- **Spring Cloud** 2023.0.1
- **Java** 17
- **MySQL**
- **Lombok**

## 模块说明

| 模块 | 说明 |
|------|------|
| lab-user | 用户服务模块 |

## 项目规划

1. **基础搭建** — 连接 MySQL，实现简单的 CRUD
2. **模块拆分** — 按商城业务拆分多个微服务模块（用户、商品、订单等）
3. **进阶体验** — 引入分布式调用与各类分布式组件（Nacos、Sentinel、Gateway 等）

## 快速开始

```bash
# 克隆项目
git clone https://github.com/liwanxing/spring-cloud-lab.git

# 进入项目目录
cd spring-cloud-lab

# 进入某个模块并启动
cd lab-user
mvn spring-boot:run
```
