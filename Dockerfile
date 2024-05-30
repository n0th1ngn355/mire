FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/mire_game.jar .

CMD ["java", "-jar", "mire_game.jar"]