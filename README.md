# Spring Cloud Lab

基于 Spring Boot 3.2.5 + Spring Cloud 的分布式商城学习项目，用于实践微服务架构与分布式组件。

## 技术栈

- **Spring Boot** 3.2.5
- **Spring Cloud** 2023.0.1
- **Java** 21
- **MyBatis-Plus** 3.5.7
- **MySQL** 8.0
- **Lombok**
- **Docker Compose**

## 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | 123456 | 管理员 |
| test01 | 123456 | 测试用户 |
| test02 | 123456 | 测试用户 |

> 密码使用 BCrypt 加密存储，数据库中为密文。

## 模块说明

| 模块 | 说明 |
|------|------|
| lab-user | 用户服务模块 |

## 项目结构

```
lab-user/
├── controller/    # 控制层
├── service/       # 服务层（接口 + 实现）
├── mapper/        # MyBatis-Plus Mapper
├── entity/        # 实体类
├── dto/           # 数据传输对象（创建、更新、响应）
├── common/        # 通用类（Result、PageResult、Assert）
├── exception/     # 统一异常处理
├── config/        # 配置类
└── util/          # 工具类
```

## 项目规划

1. **基础搭建** — 连接 MySQL，实现简单的 CRUD
2. **模块拆分** — 按商城业务拆分多个微服务模块（用户、商品、订单等）
3. **进阶体验** — 引入分布式调用与各类分布式组件（Nacos、Sentinel、Gateway 等）

## 快速开始

### 1. 启动 MySQL

```bash
cd docker
docker-compose up -d
```

### 2. 启动应用

在 IDEA 中运行 `LabUserApplication`，或：

```bash
cd lab-user
mvn spring-boot:run
```

### 3. 测试接口

```bash
# 分页查询用户
GET http://localhost:8080/api/users?page=1&size=10

# 查询单个用户
GET http://localhost:8080/api/users/1

# 模糊搜索（XML方式）
GET http://localhost:8080/api/users/search?username=admin

# 创建用户
POST http://localhost:8080/api/users
{
    "username": "newuser",
    "password": "123456",
    "email": "new@example.com",
    "phone": "13800138000"
}

# 更新用户
PUT http://localhost:8080/api/users/1
{
    "username": "updated",
    "email": "updated@example.com"
}

# 删除用户
DELETE http://localhost:8080/api/users/1
```

### 4. 运行测试

在 IDEA 中右键运行 `LabUserApplicationTests`，或：

```bash
cd lab-user
mvn test
```
