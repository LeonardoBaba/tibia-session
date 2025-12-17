# Estágio 1: Build (Compilação)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copia apenas os arquivos de dependência primeiro para aproveitar o cache do Docker
COPY .mvn/ .mvn
# Copia mvnw e pom.xml
COPY mvnw pom.xml ./

# --- ADICIONE ESTA LINHA ABAIXO ---
# Dá permissão de execução para o script mvnw
RUN chmod +x mvnw
# ----------------------------------

# Agora pode rodar sem erro
RUN ./mvnw dependency:go-offline

# Copia o código fonte e compila
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime (Execução)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Define as variáveis de ambiente com valores padrão (podem ser sobrescritos)
ENV ANALYZER_SERVER_PORT=15600

# Expõe a porta que a aplicação vai rodar
EXPOSE ${ANALYZER_SERVER_PORT}

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]