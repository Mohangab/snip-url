FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 snip
COPY --from=build /src/target/snip-url-*.jar app.jar
USER snip
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
