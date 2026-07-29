# ---- ビルドステージ ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 依存関係を先にキャッシュさせるため、pom.xmlだけ先にコピー
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# ソース一式をコピーしてビルド
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ---- 実行ステージ ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]