# 学成在线微服务后端

本仓库是“学成在线”项目的后端微服务代码，基于 Spring Boot、Spring Cloud、Spring Cloud Alibaba 和 Maven 多模块工程组织。项目按业务领域拆分为网关、认证、课程内容、媒资、搜索、学习中心、订单、系统管理、验证码、消息 SDK 等模块。

## 技术栈

- Java 8
- Spring Boot 2.3.7.RELEASE
- Spring Cloud Hoxton.SR9
- Spring Cloud Alibaba 2.2.6.RELEASE
- Maven
- MyBatis-Plus
- MySQL
- Nacos
- Redis / Redisson
- RabbitMQ
- MinIO
- Elasticsearch 7.12.1
- XXL-JOB
- Spring Security / OAuth2
- Swagger
- Log4j2

## 项目结构

```text
.
├── xuecheng-plus-parent        # 父工程，统一管理依赖版本和聚合模块
├── xuecheng-plus-base          # 公共基础包，包含通用模型、异常、工具类
├── xuecheng-plus-gateway       # API 网关，统一路由和认证过滤
├── xuecheng-plus-auth          # 认证授权服务
├── xuecheng-plus-checkcode     # 验证码服务
├── xuecheng-plus-content       # 课程内容服务
│   ├── xuecheng-plus-content-api
│   ├── xuecheng-plus-content-service
│   └── xuecheng-plus-content-model
├── xuecheng-plus-media         # 媒资服务，包含文件上传、视频处理、MinIO 集成
│   ├── xuecheng-plus-media-api
│   ├── xuecheng-plus-media-service
│   └── xuecheng-plus-media-model
├── xuecheng-plus-search        # 课程搜索服务，集成 Elasticsearch
├── xuecheng-plus-learning      # 学习中心服务
│   ├── xuecheng-plus-learning-api
│   ├── xuecheng-plus-learning-service
│   └── xuecheng-plus-learning-model
├── xuecheng-plus-orders        # 订单与支付服务
│   ├── xuecheng-plus-orders-api
│   ├── xuecheng-plus-orders-service
│   └── xuecheng-plus-orders-model
├── xuecheng-plus-system        # 系统管理服务
│   ├── xuecheng-plus-system-api
│   ├── xuecheng-plus-system-service
│   └── xuecheng-plus-system-model
├── xuecheng-plus-message-sdk   # 消息任务 SDK
├── xuecheng-plus-generator     # MyBatis-Plus 代码生成器
├── document                    # 项目文档
└── logs                        # 本地日志目录
```

## 环境准备

启动项目前，请先准备以下基础组件，并确保配置与本地环境一致：

- JDK 1.8
- Maven 3.x
- MySQL
- Nacos
- Redis
- RabbitMQ
- MinIO
- Elasticsearch 7.12.x
- XXL-JOB Admin

当前代码中的部分配置指向固定内网地址，例如：

- Nacos：`192.168.211.141:8848`
- MySQL / Redis：部分模块配置中使用 `192.168.127.121`

如果本地环境地址不同，请修改各模块的 `bootstrap.yml`，或在 Nacos 中维护对应的配置文件。

## Nacos 配置

各服务默认使用 `dev` 命名空间和以下分组：

- 项目配置分组：`xuecheng-plus-project`
- 公共配置分组：`xuecheng-plus-common`

常见公共配置文件包括：

- `logging-dev.yaml`
- `swagger-dev.yaml`
- `feign-dev.yaml`
- `redis-dev.yaml`
- `rabbitmq-dev.yaml`
- `freemarker-config-dev.yaml`

常见业务配置文件包括：

- `content-service-dev.yaml`
- `media-service-dev.yaml`
- `orders-service-dev.yaml`
- `learning-service-dev.yaml`
- `system-service-dev.yaml`

## 构建项目

在根目录执行：

```bash
mvn -f xuecheng-plus-parent/pom.xml clean install
```

如需跳过测试：

```bash
mvn -f xuecheng-plus-parent/pom.xml clean install -DskipTests
```

也可以单独构建某个模块，例如课程内容服务：

```bash
mvn -f xuecheng-plus-content/pom.xml clean install -DskipTests
```

## 启动服务

建议先启动基础设施：

1. MySQL
2. Nacos
3. Redis
4. RabbitMQ
5. MinIO
6. Elasticsearch
7. XXL-JOB Admin

然后启动后端服务。常用启动类如下：

| 服务 | 启动类 |
| --- | --- |
| 网关 | `com.xuecheng.GatewayApplication` |
| 认证服务 | `com.xuecheng.AuthApplication` |
| 验证码服务 | `com.xuecheng.checkcode.CheckcodeApplication` |
| 课程内容服务 | `com.xuecheng.ContentApplication` |
| 媒资服务 | `com.xuecheng.MediaApplication` |
| 搜索服务 | `com.xuecheng.SearchApplication` |
| 学习中心服务 | `com.xuecheng.LearningApiApplication` |
| 订单服务 | `com.xuecheng.OrdersApiApplication` |
| 系统管理服务 | `com.xuecheng.system.SystemApplication` |

使用 Maven 启动示例：

```bash
mvn -f xuecheng-plus-gateway/pom.xml spring-boot:run
mvn -f xuecheng-plus-auth/pom.xml spring-boot:run
mvn -f xuecheng-plus-content/xuecheng-plus-content-api/pom.xml spring-boot:run
mvn -f xuecheng-plus-media/xuecheng-plus-media-api/pom.xml spring-boot:run
```

也可以在 IntelliJ IDEA 中导入 `xuecheng-plus-parent/pom.xml`，等待 Maven 依赖加载完成后，直接运行对应启动类。

## 开发说明

- 统一依赖版本在 `xuecheng-plus-parent/pom.xml` 中维护。
- 接口层通常位于各业务模块的 `*-api` 子模块。
- 业务逻辑通常位于各业务模块的 `*-service` 子模块。
- 数据对象、DTO、PO 通常位于各业务模块的 `*-model` 子模块。
- 公共响应、分页、异常和工具类位于 `xuecheng-plus-base`。
- 分布式消息任务相关公共能力位于 `xuecheng-plus-message-sdk`。
- 代码生成器位于 `xuecheng-plus-generator`，用于根据数据库表生成基础代码。

## 常用命令

```bash
# 全量编译
mvn -f xuecheng-plus-parent/pom.xml clean compile

# 全量打包
mvn -f xuecheng-plus-parent/pom.xml clean package -DskipTests

# 运行测试
mvn -f xuecheng-plus-parent/pom.xml test

# 单模块打包
mvn -f xuecheng-plus-media/pom.xml clean package -DskipTests
```

## 注意事项

- 项目依赖 Nacos 配置中心，服务启动前需要确认命名空间、分组和配置文件已创建。
- 部分模块依赖远程 MySQL、Redis、MinIO、Elasticsearch 等地址，首次运行时建议统一替换为本地或测试环境地址。
- 认证相关服务使用 Spring Security / OAuth2，访问业务接口时通常需要携带有效令牌。
- 媒资、课程发布、订单、学习中心等模块之间存在 Feign 调用和消息任务依赖，联调时建议配套启动相关服务。
