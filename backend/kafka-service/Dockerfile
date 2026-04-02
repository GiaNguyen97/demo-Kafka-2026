FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*1.0.0.jar
COPY ${JAR_FILE} app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-jar","/app.jar"]
