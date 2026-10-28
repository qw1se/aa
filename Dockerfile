# Этап 1: Сборка фронтенда (React + Vite)
FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Этап 2: Сборка бэкенда (Java + Maven) и интеграция
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src/

# Копируем собранный React в статические ресурсы Spring Boot
RUN mkdir -p /app/backend/src/main/resources/static
COPY --from=frontend-build /app/frontend/dist /app/backend/src/main/resources/static

# Сборка JAR-файла через Maven
WORKDIR /app/backend
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

# Этап 3: Финальный образ для запуска
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/alchemy-web-1.0.0.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
