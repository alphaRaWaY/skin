FROM maven:3.9.15-eclipse-temurin-17 AS builder

ARG SERVICE_MODULE
WORKDIR /workspace

COPY . .
RUN mvn -B -pl ${SERVICE_MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine

ARG SERVICE_MODULE
WORKDIR /app

COPY --from=builder /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar app.jar

EXPOSE 19080 19081 19082 19083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]