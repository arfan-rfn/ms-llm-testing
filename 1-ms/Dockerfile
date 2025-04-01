FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app
COPY pom.xml .
COPY src src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]