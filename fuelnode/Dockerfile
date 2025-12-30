FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built application from local build
COPY build/install/knt_fuelnode /app

# Ensure execution permissions
RUN chmod +x /app/bin/knt_fuelnode

EXPOSE 80

CMD ["/app/bin/knt_fuelnode"]