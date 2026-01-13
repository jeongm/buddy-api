# 1. 자바 실행 환경(JDK) 가져오기
FROM openjdk:17-jdk-slim

# 2. 빌드된 jar 파일을 컨테이너 내부로 복사
# (빌드 결과물 위치는 프로젝트 설정에 따라 다를 수 있음)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 3. 서버 실행 명령
ENTRYPOINT ["java", "-jar", "/app.jar"]