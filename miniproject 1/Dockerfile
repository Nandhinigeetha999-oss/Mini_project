# Dockerfile for Smart Road Maintenance System
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .

# Compile Java source code
RUN mkdir -p classes && \
    find src -name "*.java" > sources.txt && \
    javac -d classes -cp "classes:lib/*" @sources.txt

# Production runtime container
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/classes ./classes
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/Front-End ./Front-End
COPY --from=builder /app/smart_road_maintenance.db ./smart_road_maintenance.db

# Dynamic port assigned by cloud hosts (Render, Railway, Heroku)
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "classes:lib/*", "Main"]
