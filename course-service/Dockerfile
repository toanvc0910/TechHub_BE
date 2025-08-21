# Sử dụng image Java JRE nhẹ
FROM openjdk:11-jre-slim

# Biến truyền vào khi build
ARG PROJECT_VERSION=0.1.0

# Thiết lập timezone
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Tạo thư mục ứng dụng
WORKDIR /home/app

# Copy file JAR vào container (đổi đúng tên jar course-service)
COPY target/course-service-0.0.1-SNAPSHOT.jar app.jar

# Biến môi trường cho Spring profile
ENV SPRING_PROFILES_ACTIVE=dev

# Expose port (ví dụ course-service chạy trên 8082)
EXPOSE 8082

# Chạy app
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]
