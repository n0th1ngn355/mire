FROM openjdk:17-slim

WORKDIR /app

COPY . .

CMD ["lein", "run"]
