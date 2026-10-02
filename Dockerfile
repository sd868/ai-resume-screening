FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN javac -encoding UTF-8 -d out *.java

CMD ["java", "-cp", "out", "ResumeServer"]
