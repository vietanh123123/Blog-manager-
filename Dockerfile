FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY src ./src
COPY lib ./lib

RUN mkdir -p out \
    && javac -cp "lib/postgresql-42.7.3.jar:lib/bcrypt-0.10.2.jar:lib/bytes-1.6.1.jar" -d out $(find src -name '*.java') \
    && cp src/db.properties out/

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/out ./out
COPY --from=build /app/lib ./lib

EXPOSE 8080

CMD ["java", "-cp", "out:lib/postgresql-42.7.3.jar:lib/bcrypt-0.10.2.jar:lib/bytes-1.6.1.jar", "com.blog.server.BlogServer"]