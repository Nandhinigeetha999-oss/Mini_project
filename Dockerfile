# Root Dockerfile for Smart Road Maintenance System
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY "miniproject 1" ./project

WORKDIR /app/project
RUN mkdir -p classes && \
    find src -name "*.java" > sources.txt && \
    javac -d classes -cp "classes:lib/*" @sources.txt

# Production runtime container
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/project/classes ./classes
COPY --from=builder /app/project/lib ./lib
COPY --from=builder /app/project/Front-End ./Front-End
COPY --from=builder /app/project/smart_road_maintenance.db ./smart_road_maintenance.db

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "classes:lib/*", "Main"]
