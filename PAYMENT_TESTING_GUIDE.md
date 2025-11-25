# Hướng dẫn Test Payment với Postman

## 🎯 Tổng quan
Sau khi cập nhật, hệ thống thanh toán đã tích hợp tự động tạo enrollment khi thanh toán thành công. Bạn cần truyền thêm tham số `courseId` để hệ thống biết user đang thanh toán cho khóa học nào.

---

## 📝 API Endpoints đã cập nhật

### 1. VNPay Payment

**Endpoint:** `GET http://localhost:8443/app/api/proxy/payments/vn-pay`

**Query Parameters (Required):**
- `amount`: Số tiền thanh toán (VNĐ) - ví dụ: `100000`
- `userId`: UUID của user - ví dụ: `123e4567-e89b-12d3-a456-426614174000`
- `courseId`: UUID của khóa học - ví dụ: `987fcdeb-51a2-43d1-b123-123456789abc`
- `bankCode`: Mã ngân hàng (optional) - ví dụ: `NCB`

**Ví dụ request trong Postman:**
```
GET http://localhost:8443/app/api/proxy/payments/vn-pay?amount=100000&userId=123e4567-e89b-12d3-a456-426614174000&courseId=987fcdeb-51a2-43d1-b123-123456789abc&bankCode=NCB
```

**Response:**
```json
{
    "httpCode": 200,
    "message": "Success",
    "result": {
        "code": "ok",
        "message": "success",
        "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10000000&..."
    }
}
```

---

### 2. PayPal Payment

**Endpoint:** `POST http://localhost:8443/app/api/proxy/payments/paypal/create`

**Request Body (Form Data hoặc x-www-form-urlencoded):**
- `amount`: Số tiền thanh toán (USD) - ví dụ: `10.00`
- `userId`: UUID của user - ví dụ: `123e4567-e89b-12d3-a456-426614174000`
- `courseId`: UUID của khóa học - ví dụ: `987fcdeb-51a2-43d1-b123-123456789abc`

**Ví dụ trong Postman:**
1. Chọn method: `POST`
2. URL: `http://localhost:8443/app/api/proxy/payments/paypal/create`
3. Tab Body → chọn `x-www-form-urlencoded`
4. Thêm các key-value:
   - `amount` = `10.00`
   - `userId` = `123e4567-e89b-12d3-a456-426614174000`
   - `courseId` = `987fcdeb-51a2-43d1-b123-123456789abc`

**Response:**
```json
{
    "id": "8XK12345ABCD6789",
    "status": "CREATED",
    "links": [
        {
            "href": "https://www.sandbox.paypal.com/checkoutnow?token=8XK12345ABCD6789",
            "rel": "approve",
            "method": "GET"
        }
    ],
    "transaction_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 🔄 Luồng thanh toán hoàn chỉnh

### VNPay Flow:
```
1. Postman gọi API: GET /vn-pay?amount=100000&userId={userId}&courseId={courseId}
   ↓
2. Backend tạo Transaction (PENDING) + TransactionItem (chứa courseId)
   ↓
3. Backend trả về paymentUrl của VNPay
   ↓
4. User mở paymentUrl trên browser và thanh toán
   ↓
5. VNPay callback về: GET /vn-pay-callback
   ↓
6. Backend:
   - Cập nhật Transaction status = COMPLETED
   - Lưu Payment record
   - Tự động gọi Course Service để tạo Enrollment
   ↓
7. User được enroll vào course thành công! ✅
```

### PayPal Flow:
```
1. Postman gọi API: POST /paypal/create (body: amount, userId, courseId)
   ↓
2. Backend tạo Transaction (PENDING) + TransactionItem (chứa courseId)
   ↓
3. Backend tạo PayPal order và trả về approve link
   ↓
4. User mở approve link trên browser và thanh toán
   ↓
5. PayPal redirect về: GET /paypal/success?token={token}
   ↓
6. Backend:
   - Capture payment từ PayPal
   - Cập nhật Transaction status = COMPLETED
   - Lưu Payment record
   - Tự động gọi Course Service để tạo Enrollment
   ↓
7. User được enroll vào course thành công! ✅
```

---

## 🗄️ Dữ liệu được lưu vào Database

### Bảng `transactions`:
```sql
id                  | user_id             | amount  | status    | created
--------------------|---------------------|---------|-----------|-------------------
{transaction_uuid}  | {user_uuid}         | 100000  | COMPLETED | 2025-11-23 14:30:00
```

### Bảng `transaction_items`:
```sql
id              | transaction_id      | course_id           | price_at_purchase | quantity
----------------|---------------------|---------------------|-------------------|----------
{item_uuid}     | {transaction_uuid}  | {course_uuid}       | 100000           | 1
```

### Bảng `payments`:
```sql
id              | transaction_id      | method  | status  | gateway_response
----------------|---------------------|---------|---------|------------------
{payment_uuid}  | {transaction_uuid}  | VNPAY   | SUCCESS | {...json...}
```

### Bảng `enrollments` (tự động tạo):
```sql
id              | user_id      | course_id     | status   | enrolled_at
----------------|--------------|---------------|----------|-------------------
{enroll_uuid}   | {user_uuid}  | {course_uuid} | ENROLLED | 2025-11-23 14:30:05
```

---

## 🧪 Test trong Postman - Các bước chi tiết

### Bước 1: Lấy thông tin test
Truy vấn database để lấy:
```sql
-- Lấy userId
SELECT id, email FROM users LIMIT 1;

-- Lấy courseId
SELECT id, title, price FROM courses WHERE status = 'PUBLISHED' LIMIT 1;
```

### Bước 2: Test VNPay
1. Tạo request mới trong Postman
2. Method: `GET`
3. URL: `http://localhost:8443/app/api/proxy/payments/vn-pay`
4. Params:
   - `amount` = giá của course (ví dụ: `100000`)
   - `userId` = UUID từ bước 1
   - `courseId` = UUID từ bước 1
   - `bankCode` = `NCB` (optional)
5. Send request
6. Copy `paymentUrl` từ response
7. Mở URL đó trên browser
8. Thanh toán với thông tin test VNPay:
   - Số thẻ: `9704198526191432198`
   - Tên: `NGUYEN VAN A`
   - Ngày phát hành: `07/15`
   - Mật khẩu: `123456`

### Bước 3: Test PayPal
1. Tạo request mới trong Postman
2. Method: `POST`
3. URL: `http://localhost:8443/app/api/proxy/payments/paypal/create`
4. Body → x-www-form-urlencoded:
   - `amount` = giá của course chia cho 25000 (convert VNĐ sang USD)
   - `userId` = UUID từ bước 1
   - `courseId` = UUID từ bước 1
5. Send request
6. Tìm link có `rel: "approve"` trong response
7. Mở link đó trên browser
8. Đăng nhập PayPal Sandbox:
   - Email: buyer account từ PayPal Developer Dashboard
   - Password: từ PayPal Developer Dashboard

### Bước 4: Kiểm tra kết quả
Sau khi thanh toán thành công, kiểm tra database:

```sql
-- Kiểm tra transaction
SELECT * FROM transactions 
WHERE user_id = '{user_uuid}' 
ORDER BY created DESC LIMIT 1;

-- Kiểm tra transaction_items
SELECT ti.*, c.title as course_name
FROM transaction_items ti
JOIN courses c ON ti.course_id = c.id
WHERE ti.transaction_id = '{transaction_uuid}';

-- Kiểm tra payment
SELECT * FROM payments 
WHERE transaction_id = '{transaction_uuid}';

-- Kiểm tra enrollment (quan trọng nhất!)
SELECT e.*, c.title as course_name
FROM enrollments e
JOIN courses c ON e.course_id = c.id
WHERE e.user_id = '{user_uuid}' 
  AND e.course_id = '{course_uuid}';
```

Nếu thấy record trong bảng `enrollments` → Thành công! 🎉

---

## ⚠️ Lưu ý quan trọng

1. **courseId là bắt buộc**: Nếu không truyền courseId, API sẽ trả về lỗi:
   ```json
   {
       "message": "courseId parameter is required for VNPay payment"
   }
   ```

2. **userId là bắt buộc**: Tương tự courseId, userId cũng bắt buộc phải có.

3. **UUID phải hợp lệ**: Nếu truyền UUID sai format, API sẽ trả về:
   ```json
   {
       "message": "Invalid courseId format: {courseId}"
   }
   ```

4. **Course phải tồn tại**: Khi tạo enrollment, nếu courseId không tồn tại trong database, sẽ báo lỗi nhưng transaction vẫn được lưu (để có thể xử lý sau).

5. **Không tạo enrollment trùng**: Nếu user đã enroll course rồi, hệ thống sẽ trả về enrollment hiện có thay vì tạo mới.

---

## 🔍 Debug và Troubleshooting

### Xem log khi thanh toán:
```
Payment Service logs:
- "Processing payment for user: {userId}"
- "Processing payment for course: {courseId}"
- "Created transaction item for course: {courseId}"
- "Payment successful, creating enrollments for transaction: {transactionId}"

Course Service logs:
- "Received request to create enrollment for user: {userId} and course: {courseId}"
- "Successfully created enrollment with ID: {enrollmentId}"
```

### Nếu không thấy enrollment được tạo:
1. Kiểm tra log của Payment Service xem có lỗi gì không
2. Kiểm tra Course Service có chạy không (port 8082)
3. Kiểm tra biến môi trường `COURSE_SERVICE_URL` trong dev.env
4. Kiểm tra courseId có tồn tại trong bảng courses không

---

## 🚀 Tóm tắt

**Trước đây:** 
- API chỉ nhận `amount` và `userId`
- Không biết user mua course nào
- Phải tạo enrollment thủ công

**Bây giờ:**
- API nhận thêm `courseId`
- Hệ thống lưu courseId vào `transaction_items`
- Khi thanh toán thành công, tự động tạo enrollment
- User có thể truy cập course ngay lập tức! 🎓

Chúc bạn test thành công! 🎉

