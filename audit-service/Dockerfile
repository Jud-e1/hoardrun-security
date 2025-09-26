FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -q -DskipTests -pl audit-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/audit-service/target/audit-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]


