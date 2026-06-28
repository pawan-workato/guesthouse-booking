FROM gradle:8.12-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./
COPY backend ./backend
RUN ./gradlew :backend:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/install/backend /app
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["/app/bin/backend"]
