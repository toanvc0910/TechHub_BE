# Use OpenJDK JRE 11 as runtime base image
FROM eclipse-temurin:11-jre

# Optional build argument for traceability
ARG PROJECT_VERSION=0.1.0

# Configure timezone inside the container
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Application working directory
WORKDIR /home/app

# Copy the built Spring Boot JAR into the container
COPY target/ai-service-0.0.1-SNAPSHOT.jar app.jar

# Default Spring profile
ENV SPRING_PROFILES_ACTIVE=dev

# Expose the port used by ai-service
EXPOSE 8091

# Launch the service
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]