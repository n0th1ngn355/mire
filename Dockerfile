# Используем базовый образ с Java
FROM openjdk:17-slim

# Устанавливаем переменную среды для указания на версию Clojure
ENV CLOJURE_VERSION=1.10.3.814

# Устанавливаем утилиты curl и wget
RUN apt-get update && apt-get install -y curl wget

# Устанавливаем Clojure
RUN curl -O https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
    chmod +x linux-install-$CLOJURE_VERSION.sh && \
    ./linux-install-$CLOJURE_VERSION.sh && \
    rm linux-install-$CLOJURE_VERSION.sh

# Устанавливаем Leiningen
RUN curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && \
    chmod +x lein && \
    mv lein /usr/local/bin/

# Создаем директорию для приложения внутри контейнера
WORKDIR /app

# Копируем все файлы из текущей директории внутрь контейнера
COPY . .

# Запускаем сервер Mire
CMD ["lein", "run"]
