FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -q -DskipTests -pl gateway -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/gateway/target/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]


