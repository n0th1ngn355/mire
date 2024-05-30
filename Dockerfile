FROM clojure:openjdk-11-tools-deps

WORKDIR /app

COPY target/mire_game.jar .

CMD ["java", "-jar", "mire_game.jar"]