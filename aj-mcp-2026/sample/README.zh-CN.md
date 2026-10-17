# 跑通第一个 MCP 工具

这里有两个可以直接运行的工程：`server` 是 Spring Boot 程序，提供工具；`client` 是普通 Java 程序，负责调用。两个工程都兼容 Java 8，使用 MCP `2026-07-28`，不需要数据库或模型 API Key。

源码仓库：[GitHub](https://github.com/lightweight-component/aj-mcp)、[Gitee](https://gitee.com/lightweight-components/aj-mcp)、[GitCode](https://gitcode.com/lightweight-component/aj-mcp)。示例位于 `aj-mcp-2026/sample`。

## 准备依赖

自己的服务端项目添加：

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-next-server</artifactId>
    <version>1.0</version>
</dependency>
```

客户端项目添加：

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-next-client</artifactId>
    <version>1.0</version>
</dependency>
```

共用的 `aj-mcp-next-core` 会自动引入。本示例的服务端还使用 `spring-boot-starter:2.7.18`，沿用仓库的 Java 8 基线。POM 显式对齐 Jackson 版本，避免 Boot 的依赖管理将 SDK 所需版本降级。

## 构建和运行

以下命令均从仓库根目录执行，需要已安装的 Maven 和 JDK 8 或 17。首次构建需要能够下载 Maven 依赖。

```sh
git clone https://github.com/lightweight-component/aj-mcp.git
cd aj-mcp
mvn -f aj-mcp-2026/pom.xml -pl sample/server,sample/client -am -DskipTests package
```

命令会同时构建当前源码中的 SDK 和示例，避免误用本地仓库里的旧 SDK。这里跳过测试只是为了快速打包，不代表测试已经执行。如果独立开发其他工程，可先用 `mvn -f aj-mcp-2026/pom.xml -DskipTests install` 安装本地依赖。

先在一个终端启动服务端：

```sh
java -jar aj-mcp-2026/sample/server/target/aj-mcp-next-sample-server-1.0.jar
```

然后在另一个终端启动客户端：

```sh
java -jar aj-mcp-2026/sample/client/target/aj-mcp-next-sample-client-1.0.jar
```

客户端会打印服务信息、工具列表，以及类似下面的结果：

```text
发现工具：echo
发现工具：query_order
工具返回：你好，MCP！
工具返回：订单 A1001 已发货，承运方：示例快递，运单号：DEMO1001。
```

订单数据是写在示例里的。换一个订单号，可以看看业务失败的返回：

```sh
java -jar aj-mcp-2026/sample/client/target/aj-mcp-next-sample-client-1.0.jar http://127.0.0.1:8080/mcp UNKNOWN
```

如果端口被占用，两端一起改成其他端口：

```sh
java -jar aj-mcp-2026/sample/server/target/aj-mcp-next-sample-server-1.0.jar --mcp.port=8081
java -jar aj-mcp-2026/sample/client/target/aj-mcp-next-sample-client-1.0.jar http://127.0.0.1:8081/mcp
```

结束服务端时按 Ctrl+C。Spring 会先关闭 HTTP 入口，再关闭 MCP 服务及其线程池；客户端完成调用后自行退出。

## 从哪里看代码

先看 `server` 中的 `SampleServerApplication`。`mcpServer()` 描述工具需要的参数，再注册对应的处理方法。可以把 `query_order` 中的模拟判断换成现有订单 Service 的查询。

`mcpHttpTransport()` 将工具开放为 `http://127.0.0.1:8080/mcp`。这个示例由 Spring Boot 管理 Bean，HTTP 请求由 SDK 自带的 JDK HTTP 适配器处理，因此没有 MVC Controller，也没有第二个 Tomcat 端口。端口配置使用 `mcp.port`。若要接入现有 Spring MVC 应用，可另写基于 `server.connect(...)` 的适配层。

再看 `client` 中的 `SampleClient`：先发现服务、列出工具，再调用 `echo` 和 `query_order`。客户端独立出来，方便排查服务端是否可用，也方便教程分别介绍两端的依赖。

浏览器直接打开 `/mcp` 会得到 405，因为这个入口接收 MCP POST 请求。请使用配套客户端调用。接入 AI 应用前，也需要确认其支持相同的协议版本。

默认仅监听本机且没有认证，适合本地学习。开放远程访问时，需要接入可信身份认证、HTTPS 和相应网络限制。

[English](README.md)
