# Base image
FROM alpine:3.20

# Define the Maven version you want to install
ARG MAVEN_VERSION=3.8.8

# Set environment variables for Maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

# Install dependencies: OpenJDK 11 and Maven
RUN apk update && \
    apk add --no-cache openjdk11-jre curl tar && \
    mkdir -p /opt && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xzC /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} $MAVEN_HOME

# Define the project version as an argument (optional)
ARG PROJECT_VERSION=0.1.0

# Default command to show Maven version (optional, can be overridden during runtime)
CMD ["mvn", "--version"]
