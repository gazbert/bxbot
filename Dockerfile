FROM openjdk:11
RUN apt-get update
RUN apt-get install -y maven

COPY . bxbot-staging

WORKDIR ./bxbot-staging
RUN mvn clean assembly:assembly
RUN cp ./target/bxbot-parent-*-dist.tar.gz /

WORKDIR /
RUN tar -xzf bxbot-parent-*-dist.tar.gz
RUN rm bxbot-parent-*-dist.tar.gz
RUN rm -rf ./bxbot-staging