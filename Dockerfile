FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/mire_game.jar .

CMD ["java", "-jar", "mire_game.jar"]