# File Service - TechHub Backend

Service quản lý upload và lưu trữ file (images, videos, documents) lên Cloudinary.

## Features

- Upload single/multiple files
- Store file metadata in PostgreSQL
- Store actual files in Cloudinary
- Support images (JPEG, PNG, GIF, WEBP)
- Support videos (MP4, MPEG, QuickTime, WEBM)
- Max file size: 100MB
- File validation
- Delete files from Cloudinary and database

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

### 2. Configure Cloudinary

1. Sign up for free account at https://cloudinary.com
2. Get your credentials from Dashboard
3. Update `application.yml`:

```yaml
cloudinary:
  cloud-name: your_cloud_name
  api-key: your_api_key
  api-secret: your_api_secret
```

Or use environment variables:

```bash
export CLOUDINARY_CLOUD_NAME=your_cloud_name
export CLOUDINARY_API_KEY=your_api_key
export CLOUDINARY_API_SECRET=your_api_secret
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
    "url": "https://res.cloudinary.com/.../image.jpg",
    "publicId": "techhub/blogs/abc123",
    "originalFilename": "image.jpg",
    "fileType": "IMAGE",
    "size": 102400,
    "format": "jpg",
    "folder": "blogs",
    "uploadedAt": "2024-01-15T10:30:00"
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
  return result.data.url; // Cloudinary URL
}
```

## Folder Structure

Files are organized in Cloudinary by folder:

- `techhub/blogs` - Blog images and videos
- `techhub/courses` - Course materials
- `techhub/avatars` - User avatars
- `techhub/uploads` - General uploads

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

### Cloudinary upload error

- Verify credentials are correct
- Check network connection
- Ensure file size < 100MB
- Verify file type is supported

## Production Deployment

1. **Use environment variables** for sensitive data:

```bash
export CLOUDINARY_CLOUD_NAME=prod_cloud_name
export CLOUDINARY_API_KEY=prod_api_key
export CLOUDINARY_API_SECRET=prod_api_secret
export DB_URL=jdbc:postgresql://prod-db:5432/techhub_file
export DB_USERNAME=prod_user
export DB_PASSWORD=prod_password
```

2. **Update CORS** in FileController for production domains

3. **Configure API Gateway** to route `/api/files/*` to file-service

4. **Monitor Cloudinary usage** to stay within free tier limits (25GB storage, 25GB bandwidth/month)

## Tech Stack

- Spring Boot 2.5.14
- Spring Cloud Netflix Eureka
- PostgreSQL
- Cloudinary Java SDK 1.38.0
- Lombok
- Maven

## License

MIT
