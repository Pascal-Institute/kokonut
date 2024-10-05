# Use the official Kotlin image as the base image
FROM gradle:jdk17 AS builder

# Set the working directory
WORKDIR /app

# Copy the Gradle wrapper and project files
COPY . .

# Build the application
RUN mkdir -p app/src/main/dist

RUN gradle installDist --info

# Use a lightweight Java runtime image
FROM openjdk:17-jdk-slim

# Copy the built application from the builder stage
COPY --from=builder /app/build/install/fullnode /app

RUN apt-get update && apt-get install -y git

RUN apt-get update && apt-get install -y curl

RUN chmod -R 777 /app

# Expose the port that your application will run on
EXPOSE 80

# Command to run the application
CMD ["/app/bin/fullnode"]