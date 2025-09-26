FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -q -DskipTests -pl transaction-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/transaction-service/target/transaction-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]


