# Multi-stage build for Smart Municipal Road Maintenance System
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .

# Compile Java source files
RUN mkdir -p classes && \
    find src -name "*.java" > sources.txt && \
    javac -d classes -cp "classes:lib/*" @sources.txt

# Production runtime container
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/classes ./classes
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/Front-End ./Front-End
COPY --from=builder /app/index.html ./index.html
COPY --from=builder /app/script.js ./script.js
COPY --from=builder /app/style.css ./style.css
COPY --from=builder /app/database.html ./database.html
COPY --from=builder /app/vendor ./vendor
COPY --from=builder /app/smart_road_maintenance.db ./smart_road_maintenance.db

# Dynamic port assigned by Render ($PORT)
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "classes:lib/*", "Main", "--server"]
