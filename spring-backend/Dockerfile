# Multi-stage build for optimized image
FROM eclipse-temurin:17-jdk-jammy as build

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy the entire project
COPY . .

# Copy environment file
COPY .env .env

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy environment file
COPY .env .env

# Create non-root user for security
RUN useradd -r -s /bin/false appuser && chown appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=seed

# Run the application
CMD ["java", "-jar", "app.jar"]