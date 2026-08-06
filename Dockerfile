# syntax=docker/dockerfile:1

##########################
# Estágio 1 — build (Maven + JDK 21)
##########################
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 1) Copia só o pom para cachear o download das dependências.
#    Enquanto o pom.xml não muda, essa camada é reaproveitada.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2) Copia o código-fonte e empacota o JAR executável.
#    -DskipTests: os testes rodam contra um PostgreSQL interno (10.68.100.15),
#    que não existe no ambiente de build da AWS. Rode os testes no CI/local, não aqui.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

##########################
# Estágio 2 — runtime (só JRE, imagem enxuta)
##########################
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuário sem privilégios (boa prática; a AWS não exige root)
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

# Só o JAR executável vai para a imagem final (finalName=srs-backend no pom.xml)
COPY --from=build /build/target/srs-backend.jar app.jar

EXPOSE 8080

# MaxRAMPercentage faz a JVM respeitar o limite de memória do container (ECS/Fargate).
# exec + PID 1 garante que o SIGTERM do ECS chegue na JVM (shutdown gracioso).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
