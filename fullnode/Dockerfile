FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built application from local build
COPY build/install/knt_fullnode /app

# Ensure execution permissions
RUN chmod +x /app/bin/knt_fullnode

EXPOSE 80

# Environment variables for data persistence
ENV KOKONUT_DATA_DIR=/data
VOLUME /data

CMD ["/app/bin/knt_fullnode"]