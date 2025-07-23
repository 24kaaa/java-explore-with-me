FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /workspace

# 1. Копируем только нужные POM-файлы
COPY . .

# 2. Скачиваем зависимости
RUN mvn dependency:go-offline -B

# 3. Копируем исходный код
COPY stats/stats-server/src ./stats/stats-server/src/
COPY stats/stats-dto/src ./stats/stats-dto/src/

# 4. Собираем только stats-server
RUN mvn clean package -pl stats/stats-server -am -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/stats/stats-server/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]