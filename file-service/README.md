# File Service - TechHub Backend

Service quản lý upload và lưu trữ file (images, videos, documents) lên MinIO.

## Features

- Upload single/multiple files
- Store file metadata in PostgreSQL
- Store actual files in MinIO
- Support images (JPEG, PNG, GIF, WEBP)
- Support videos (MP4, MPEG, QuickTime, WEBM)
- Max file size: 100MB
- File validation
- Delete files from MinIO and database

## Setup

### 1. Create PostgreSQL Database

```bash
psql -U postgres
```

Run the SQL script:

```bash
psql -U postgres -f schema.sql
```

Or create manually:

```sql
CREATE DATABASE techhub_file;
```

### 2. Configure MinIO

1. Start MinIO and create bucket `techhub`
2. Make the bucket publicly readable for object GET requests
3. Update `application.yml`:

```yaml
minio:
  endpoint: http://your-minio-host:9000
  access-key: your_minio_access_key
  secret-key: your_minio_secret_key
  bucket: your_bucket_name
  public-url: https://your-public-file-host
  secure: false
```

Or use environment variables:

```bash
export MINIO_ENDPOINT=http://your-minio-host:9000
export MINIO_ACCESS_KEY=your_minio_access_key
export MINIO_SECRET_KEY=your_minio_secret_key
export MINIO_BUCKET=your_bucket_name
export MINIO_PUBLIC_URL=https://your-public-file-host
export MINIO_SECURE=false
```

### 3. Update Database Connection

Edit `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://techhub-toanc4439-3d01.f.aivencloud.com:21337/techhub_file
    username: your_username
    password: your_password
```

### 4. Build and Run

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/file-service-0.0.1-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

Service will run on port **8086**.

## API Endpoints

### Upload Single File

```
POST http://localhost:8086/api/files/upload
Content-Type: multipart/form-data

Form fields:
- file: (file) - The file to upload
- folder: (text) - Optional folder name (default: "uploads")
```

**Response:**

```json
{
  "status": "success",
  "message": "File uploaded successfully",
  "data": {
    "id": 1,
    "cloudinaryUrl": "https://your-public-file-host/your_bucket_name/users/.../image.jpg",
    "cloudinaryPublicId": "users/.../image.jpg",
    "originalFilename": "image.jpg",
    "fileType": "IMAGE",
    "fileSize": 102400,
    "created": "2024-01-15T10:30:00"
  }
}
```

### Upload Multiple Files

```
POST http://localhost:8086/api/files/upload/multiple
Content-Type: multipart/form-data

Form fields:
- files: (file[]) - Multiple files
- folder: (text) - Optional folder name
```

### Delete File

```
DELETE http://localhost:8086/api/files/{publicId}
```

Example:

```
DELETE http://localhost:8086/api/files/techhub/blogs/abc123
```

### Get File Metadata

```
GET http://localhost:8086/api/files/{id}
```

## File Validation

- **Max file size:** 100MB
- **Allowed image types:** JPEG, PNG, GIF, WEBP
- **Allowed video types:** MP4, MPEG, QuickTime, WEBM

## Frontend Integration

In your React/Next.js app, use the file service like this:

```typescript
async function uploadFile(file: File, folder: string = "uploads") {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("folder", folder);

  const response = await fetch("http://localhost:8086/api/files/upload", {
    method: "POST",
    body: formData,
  });

  const result = await response.json();
  return result.data.cloudinarySecureUrl; // MinIO public URL via compatibility fields
}
```

## Folder Structure

Files are organized in MinIO by object key:

- `users/{userId}/image/*` - User images
- `users/{userId}/video/*` - User videos
- `users/{userId}/document/*` - Documents and other uploads

## Database Schema

```sql
CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    file_type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    format VARCHAR(50),
    folder VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Troubleshooting

### Port 8086 already in use

```bash
# Find process
lsof -i :8086

# Kill process
kill -9 <PID>
```

### Database connection error

- Check PostgreSQL is running: `pg_isready`
- Verify database exists: `psql -U postgres -l`
- Check credentials in application.yml

### MinIO upload error

- Verify MinIO endpoint and credentials are correct
- Verify bucket `techhub` exists and has public read policy
- Check network connection
- Ensure file size < 100MB
- Verify file type is supported

## Production Deployment

1. **Use environment variables** for sensitive data:

```bash
export MINIO_ENDPOINT=http://your-minio-internal-host:9000
export MINIO_ACCESS_KEY=prod_access_key
export MINIO_SECRET_KEY=prod_secret_key
export MINIO_BUCKET=prod_bucket_name
export MINIO_PUBLIC_URL=https://files.example.com
export MINIO_SECURE=true
export DB_URL=jdbc:postgresql://prod-db:5432/techhub_file
export DB_USERNAME=prod_user
export DB_PASSWORD=prod_password
```

2. **Update CORS** in FileController for production domains

3. **Configure API Gateway** to route `/api/files/*` to file-service

4. **Monitor MinIO usage** and bucket growth

## Tech Stack

- Spring Boot 2.5.14
- Spring Cloud Netflix Eureka
- PostgreSQL
- MinIO Java SDK 8.5.17
- Lombok
- Maven

## License

MIT
