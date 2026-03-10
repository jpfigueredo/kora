FROM eclipse-temurin:21-jdk

WORKDIR /app

# Espera-se que o jar já tenha sido gerado via Maven:
# JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn clean package -DskipTests
COPY target/kafka-consumer-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","/app/app.jar"]

