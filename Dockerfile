FROM clojure:lein-2.9.6-openjdk-11

WORKDIR /app

COPY target/mire_game.jar .

CMD ["java", "-jar", "mire_game.jar"]