# Use OpenJDK JRE 11 as runtime base image
FROM eclipse-temurin:17-jre

# Optional build argument for traceability
ARG PROJECT_VERSION=0.1.0

# Configure timezone inside the container
ENV TZ=Asia/Ho_Chi_Minh
RUN rm -f /etc/apt/sources.list.d/*.sources /etc/apt/sources.list.d/*.list /etc/apt/sources.list && \
    echo 'deb http://mirror.sg.gs/ubuntu noble main restricted universe multiverse' > /etc/apt/sources.list && \
    echo 'deb http://mirror.sg.gs/ubuntu noble-updates main restricted universe multiverse' >> /etc/apt/sources.list && \
    echo 'deb http://mirror.sg.gs/ubuntu noble-security main restricted universe multiverse' >> /etc/apt/sources.list && \
    apt-get update \
	&& apt-get install -y --no-install-recommends ffmpeg \
	&& rm -rf /var/lib/apt/lists/* \
	&& ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
	&& echo $TZ > /etc/timezone

# Application working directory
WORKDIR /home/app

# Copy the built Spring Boot JAR into the container
COPY target/file-service-0.0.1-SNAPSHOT.jar app.jar

# Default Spring profile
ENV SPRING_PROFILES_ACTIVE=dev

# Expose the port used by file-service
EXPOSE 8086

# Launch the service
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]