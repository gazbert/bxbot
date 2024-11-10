FROM eclipse-temurin:21-jdk

# Install editor for folks to play around with the config.
RUN apt-get update
RUN apt-get install -y vim

COPY . bxbot-staging
WORKDIR ./bxbot-staging

RUN ./mvnw clean package
RUN cp ./bxbot-app/target/bxbot-app-*-dist.tar.gz /

WORKDIR /
RUN tar -xzf bxbot-app-*-dist.tar.gz
RUN rm bxbot-app-*-dist.tar.gz
RUN rm -rf ./bxbot-staging

EXPOSE 8080
