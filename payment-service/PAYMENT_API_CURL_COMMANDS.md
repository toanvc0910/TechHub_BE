# Payment Service API - CURL Commands

**Base URL via API Gateway:** `http://localhost:8443/app/api/proxy/payments`  
**Base URL Direct:** `http://localhost:8084/api/v1/payment`

**Note:** Replace `YOUR_JWT_TOKEN` with actual JWT token from login response.

---

## 💳 1. PAYPAL PAYMENT

### 1.1 Create PayPal Order (Public)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/payments/paypal/create?amount=100.00" \
  -H "accept: application/json"
```

**Response Example:**
```json
{
  "id": "ORDER_ID",
  "status": "CREATED",
  "links": [
    {
      "href": "https://www.sandbox.paypal.com/checkoutnow?token=TOKEN",
      "rel": "approve",
      "method": "GET"
    }
  ]
}
```

### 1.2 PayPal Success Callback (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/payments/paypal/success?token=PAYPAL_TOKEN" \
  -H "accept: application/json"
```

### 1.3 PayPal Cancel (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/payments/paypal/cancel" \
  -H "accept: application/json"
```

---

## 🏦 2. VNPAY PAYMENT

### 2.1 Create VNPay Payment (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/payments/vn-pay?amount=100000&bankCode=NCB&orderInfo=Payment for course" \
  -H "accept: application/json"
```

**Query Parameters:**
- `amount` (required): Số tiền thanh toán (VND)
- `bankCode` (optional): Mã ngân hàng (NCB, VIETCOMBANK, TECHCOMBANK, etc.)
- `orderInfo` (optional): Thông tin đơn hàng

**Response Example:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "code": "00",
    "message": "success",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=..."
  }
}
```

### 2.2 VNPay Payment Callback (Public - Auto redirect)
```bash
# This endpoint is called automatically by VNPay after payment
# It will redirect to: http://localhost:3000/result?status=success&txnRef=...&amount=...

curl -X GET "http://localhost:8443/app/api/proxy/payments/vn-pay-callback?vnp_Amount=10000000&vnp_BankCode=NCB&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_TxnRef=ORDER_REF&vnp_SecureHash=HASH" \
  -H "accept: application/json"
```

---

## 📊 3. PAYMENT MANAGEMENT (Via Proxy)

### 3.1 Create Payment (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/payments/create" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "COURSE_UUID",
    "amount": 99.99,
    "currency": "USD",
    "paymentMethod": "PAYPAL"
  }'
```

### 3.2 Get Payment Status (Requires Auth)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/payments/PAYMENT_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3.3 Get Payment History (Requires Auth)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/payments/history?page=0&size=10" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🔔 4. PAYMENT CALLBACKS

### 4.1 MoMo Payment Callback (Public)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/payments/callback/momo" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerCode": "MOMO_PARTNER_CODE",
    "orderId": "ORDER_ID",
    "requestId": "REQUEST_ID",
    "amount": 100000,
    "orderInfo": "Payment for course",
    "resultCode": 0,
    "message": "Successful",
    "responseTime": 1234567890,
    "signature": "SIGNATURE_HASH"
  }'
```

### 4.2 ZaloPay Payment Callback (Public)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/payments/callback/zalopay" \
  -H "Content-Type: application/json" \
  -d '{
    "app_id": "ZALOPAY_APP_ID",
    "app_trans_id": "TRANSACTION_ID",
    "app_time": 1234567890,
    "amount": 100000,
    "status": 1,
    "checksum": "CHECKSUM_HASH"
  }'
```

---

## 🔐 Authentication Required

Most endpoints require JWT authentication. First, login to get a token:

```bash
# Login
curl -X POST "http://localhost:8443/app/api/proxy/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "yourpassword"
  }'
```

Response will include `accessToken`. Use it in subsequent requests:
```bash
-H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

---

## 📌 Notes

1. **Payment Methods:**
   - `PAYPAL` - PayPal payment gateway
   - `VNPAY` - VNPay payment gateway (Vietnam)
   - `MOMO` - MoMo e-wallet (Vietnam)
   - `ZALOPAY` - ZaloPay e-wallet (Vietnam)

2. **Payment Status:**
   - `PENDING` - Payment is being processed
   - `COMPLETED` - Payment successful
   - `FAILED` - Payment failed
   - `CANCELLED` - Payment cancelled by user

3. **Currency Codes:**
   - `USD` - US Dollar (PayPal)
   - `VND` - Vietnamese Dong (VNPay, MoMo, ZaloPay)

4. **VNPay Bank Codes:**
   - `NCB` - Ngân hàng NCB
   - `VIETCOMBANK` - Ngân hàng Vietcombank
   - `TECHCOMBANK` - Ngân hàng Techcombank
   - `BIDV` - Ngân hàng BIDV
   - `AGRIBANK` - Ngân hàng Agribank
   - ... và nhiều ngân hàng khác

5. **⚠️ IMPORTANT - PayPal Configuration:**
   - PayPal is configured in **SANDBOX** mode for testing
   - Use PayPal sandbox test accounts for testing
   - Production credentials should be configured in production environment

6. **🔄 Payment Flow:**
   - **Step 1:** Create payment order → Get payment URL
   - **Step 2:** Redirect user to payment URL
   - **Step 3:** User completes payment on payment gateway
   - **Step 4:** Payment gateway calls callback URL
   - **Step 5:** System processes callback and updates payment status
   - **Step 6:** User is redirected to result page

---

## 🧪 Quick Test Flow

### VNPay Payment Flow:
1. **Create VNPay payment** → Get `paymentUrl`
2. **Open paymentUrl in browser** → Complete payment on VNPay
3. **VNPay redirects to callback** → Auto redirect to result page
4. **Check payment status** → Verify transaction

### PayPal Payment Flow:
1. **Create PayPal order** → Get approval URL
2. **Open approval URL in browser** → Login to PayPal sandbox
3. **Approve payment** → Complete payment
4. **Redirect to success URL** → Payment captured
5. **Check payment status** → Verify transaction

---

## 🌐 Frontend Integration Example

### VNPay Payment:
```javascript
// Create VNPay payment
const response = await fetch('http://localhost:8443/app/api/proxy/payments/vn-pay?amount=100000&orderInfo=Course Payment');
const data = await response.json();

// Redirect to VNPay payment page
window.location.href = data.data.paymentUrl;

// Handle result page (after callback redirect)
// URL: http://localhost:3000/result?status=success&txnRef=...&amount=...
const urlParams = new URLSearchParams(window.location.search);
const status = urlParams.get('status'); // 'success' or 'failed'
const txnRef = urlParams.get('txnRef');
const amount = urlParams.get('amount');
```

### PayPal Payment:
```javascript
// Create PayPal order
const response = await fetch('http://localhost:8443/app/api/proxy/payments/paypal/create?amount=100.00', {
  method: 'POST'
});
const order = await response.json();

// Find approval URL
const approvalUrl = order.links.find(link => link.rel === 'approve').href;

// Redirect to PayPal
window.location.href = approvalUrl;

// PayPal will redirect back to success URL with token parameter
// Handle success callback
// URL: http://localhost:8443/app/api/proxy/payments/paypal/success?token=...
```

---

## 🔧 Configuration

### PayPal Sandbox Configuration:
- **Client ID:** `AaJznEpFe4e6d9IvlvAow5_hNb_LXF5Ux7ErjZuNlr4QRmyxMvxUlC6rBjxbUwOxAK5Ohpaodi7xY4dz`
- **Mode:** `sandbox`
- **API Base:** `https://api-m.sandbox.paypal.com`

### VNPay Configuration:
- Configure in `application.yml` or environment variables
- Required: `vnpay.tmnCode`, `vnpay.secretKey`, `vnpay.url`, `vnpay.returnUrl`

---

## 📞 Support

For payment integration issues:
- Check payment gateway documentation
- Verify callback URLs are accessible
- Ensure proper signature/hash validation
- Test with sandbox/test accounts first

---

**Last Updated:** 2025-01-15  
**Version:** 1.0.0

