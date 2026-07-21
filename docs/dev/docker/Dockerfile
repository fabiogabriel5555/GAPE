# Development image only; production TLS terminates at a hardened proxy or
# dedicated Tomcat connector documented in docs/security/https.md.
FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM tomcat:10.1.56-jdk25-temurin

RUN rm -rf /usr/local/tomcat/webapps/* \
    && mkdir -p /var/lib/gape/uploads
COPY --from=build /workspace/target/gape.war /usr/local/tomcat/webapps/GAPE.war
COPY uploads/ /var/lib/gape/uploads/

ENV TZ="Europe/Lisbon"
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Duser.timezone=Europe/Lisbon -Dgape.upload.dir=/var/lib/gape/uploads -Dgape.webp.native.dir=/var/lib/gape/.gape-webp-native"
EXPOSE 8080

CMD ["catalina.sh", "run"]
