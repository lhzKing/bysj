# --- 阶段 1：用 Maven 镜像构建 jar ---
FROM maven:3.9-eclipse-temurin-19 AS build
WORKDIR /build

# 先单独 COPY pom.xml 走依赖缓存，源码变化时不用重下依赖
COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -e -ntp clean package -DskipTests \
    && mv target/*.jar app.jar

# --- 阶段 2：精简运行镜像 ---
FROM eclipse-temurin:19-jre-jammy
WORKDIR /app

# 时区改东八区，日志/LocalDateTime 对齐
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /build/app.jar app.jar

# 容器内运行用的 RSA 密钥目录（compose 把宿主机 keys/ 挂这里）
# 注意：镜像里不内置密钥，避免泄漏
VOLUME ["/app/keys"]

EXPOSE 8080

# JVM 参数：容器内自适应内存上限；-XX:+ExitOnOutOfMemoryError 让 K8s/compose 能感知 OOM 重启
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
