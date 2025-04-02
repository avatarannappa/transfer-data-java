# 检测数据传输应用

这是一个Java应用程序，用于监控检测设备生成的CSV文件，并将最新的检测数据通过HTTP POST请求发送到指定的API端点。

## 功能特性

- 监控指定文件夹中的CSV文件更新
- 解析ISO-8859-1编码的CSV文件
- 将数据通过HTTP POST发送至指定API
- 支持失败重传和断点续传
- 图形用户界面(GUI)用于配置和状态监控
- 支持最小化到后台运行

## 技术栈

- Java 8
- Swing/JavaFX (GUI)
- Apache HttpClient (HTTP客户端)
- OpenCSV (CSV解析)
- Jackson (JSON处理)
- H2 Database (本地数据存储)
- Commons IO (文件监控)
- Maven (项目构建)
- Launch4j (生成Windows可执行文件)

## 构建与运行

### 构建

```bash
mvn clean package
```

构建后将在target目录生成以下文件：
- `transfer-data-java-1.0-SNAPSHOT-jar-with-dependencies.jar`: 可执行JAR文件
- `DataTransfer.exe`: Windows可执行文件

### 运行

Windows:
- 双击 `DataTransfer.exe` 运行应用程序

Mac/Linux:
```bash
java -jar target/transfer-data-java-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 配置说明

应用程序启动后，可以通过GUI界面配置以下参数：

- API服务器地址
- 设备代码(equipmentCode)
- 生产线代码(lineCode)
- CSV文件路径
- 监控间隔时间
- 重传设置

## 版本历史

- 1.0.0 (初始版本): 基本功能实现

## 许可证

本项目采用MIT许可证。 