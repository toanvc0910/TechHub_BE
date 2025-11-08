# Sử dụng image Java JRE nhẹ
FROM eclipse-temurin:11-jre

# Biến truyền vào khi build
ARG PROJECT_VERSION=0.1.0

# Thiết lập timezone
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Tạo thư mục ứng dụng
WORKDIR /home/app

# Copy file JAR vào container (đổi đúng tên jar user-service)
COPY target/user-service-v2.5.12.jar app.jar

# Biến môi trường cho Spring profile
ENV SPRING_PROFILES_ACTIVE=dev

# Expose port (ví dụ user-service chạy trên 8080 cho Eureka)
EXPOSE 8080

# Chạy app
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]
