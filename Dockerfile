FROM openjdk:11
COPY target/client-service-0.0.1-SNAPSHOT.jar client-service.jar
EXPOSE ${port}
ENTRYPOINT exec java -jar client-service.jar