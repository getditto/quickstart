# Dockerfile for testing Maven artifacts
# Base image with JDK 17
FROM eclipse-temurin:17-jdk

# Install useful tools for testing
RUN apt-get update && apt-get install -y \
    maven \
    gradle \
    git \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /workspace

# Default command
CMD ["/bin/bash"]
