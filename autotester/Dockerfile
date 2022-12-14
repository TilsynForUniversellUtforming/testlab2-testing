FROM eclipse-temurin:17-jre AS build
VOLUME /tmp
EXPOSE 8080
COPY target/autotester-0.0.1-SNAPSHOT.jar app.jar

RUN addgroup --system -gid 1000 testlab && adduser --system --uid 1000 --ingroup testlab testlab

user testlab

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar /app.jar