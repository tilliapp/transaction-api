FROM openjdk:latest
ADD target/scala-2.13/run.jar /run.jar
ENTRYPOINT ["java", "-Xmx512m", "-cp", "/run.jar", "app.tilli.app.ApiApp"]