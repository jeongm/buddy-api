# 1. 빌드 스테이지
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# 의존성만 먼저 복사해서 캐싱 활용 (빌드 속도 향상)
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# 2. 실행 스테이지
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 타임존 설정 (로그 시간이 한국 시간으로 나오게 함)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /app/target/*.jar app.jar

# JVM 옵션 추가 (메모리 효율화)
ENTRYPOINT ["java", "-Xmx400M", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

EXPOSE 8080