FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 AS build

ARG SERVICE_MODULE
WORKDIR /workspace

COPY . .
RUN mvn -B -pl ${SERVICE_MODULE} -am -DskipTests package

FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-jammy

ARG SERVICE_MODULE
WORKDIR /app

COPY --from=build /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-*.jar app.jar

EXPOSE 19080 19081 19082 19083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
