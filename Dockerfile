FROM maven:3.9.12-eclipse-temurin-21
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
ENTRYPOINT ["mvn", "--batch-mode", "--no-transfer-progress", "-Pmysql"]
CMD ["verify"]
