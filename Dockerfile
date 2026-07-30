# =====================================================================
# ビルドステージ：Maven + JDK17 でjarをビルドする
# =====================================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 依存関係だけ先にダウンロードしてキャッシュを効かせる
# （pom.xmlが変わらない限り、ソースを直しても依存解決からやり直さない）
COPY pom.xml .
RUN mvn -B dependency:go-offline

# ソースをコピーしてビルド
COPY src ./src
RUN mvn -B clean package -DskipTests


# =====================================================================
# 実行ステージ：JDKではなくJRE（軽量）だけを使う
# =====================================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# ビルドステージで作られたjarだけをコピー（ファイル名のバージョン部分に依存しないようワイルドカード指定）
COPY --from=build /app/target/*.jar app.jar

# Renderは起動時に環境変数 PORT を渡してくるので、それをそのままSpring Bootに渡す。
# ローカルでdocker run単体で試す場合はPORT未設定でも8080で起動する。
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Duser.timezone=Asia/Tokyo -jar app.jar --server.port=${PORT}"]
