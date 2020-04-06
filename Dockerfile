FROM adoptopenjdk/openjdk13:alpine-jre

RUN mkdir -p /opt/app

WORKDIR /opt/app

COPY ./application.conf ./target/scala-2.13/messenger-assembly-0.1.jar ./

ENTRYPOINT ["java", "-jar", "messenger-assembly-0.1.jar"]