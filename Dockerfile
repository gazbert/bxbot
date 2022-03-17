FROM openjdk:11

RUN apt-get update
RUN apt-get install -y maven

COPY . bxbot-staging

WORKDIR ./bxbot-staging
RUN mvn clean package
RUN cp ./bxbot-app/target/bxbot-app-*-dist.tar.gz /

WORKDIR /
RUN tar -xzf bxbot-app-*-dist.tar.gz
RUN rm bxbot-app-*-dist.tar.gz
RUN rm -rf ./bxbot-staging

EXPOSE 8080
