FROM farao/farao-computation-base:1.7.0

ARG JAR_FILE=gridcapa-cse-valid-app/target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
