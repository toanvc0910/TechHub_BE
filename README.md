# 📚 Hướng Dẫn Sử Dụng Giao Diện Quản Trị TechHub

## 📋 Mục Lục
1. [Giới thiệu](#giới-thiệu)
2. [Đăng nhập](#đăng-nhập)
3. [Menu Avatar - Các Nút Quản Lý](#menu-avatar---các-nút-quản-lý)
4. [Dashboard](#dashboard)
5. [Quản lý Nhân viên](#quản-lý-nhân-viên)
6. [Quản lý Vai trò](#quản-lý-vai-trò)
7. [Quản lý Quyền hạn](#quản-lý-quyền-hạn)
8. [Quản lý Khóa học](#quản-lý-khóa-học)
9. [Quản lý Lộ trình Học tập](#quản-lý-lộ-trình-học-tập)
10. [Quản lý Bài viết](#quản-lý-bài-viết)
11. [Quản lý File](#quản-lý-file)
12. [Xử lý sự cố thường gặp](#xử-lý-sự-cố-thường-gặp)

---

## 🎯 Giới thiệu

**TechHub** là nền tảng quản lý và học tập trực tuyến với giao diện quản trị toàn diện. Hệ thống hỗ trợ:
- ✅ Quản lý người dùng và phân quyền chi tiết
- ✅ Quản lý khóa học với tích hợp AI
- ✅ Quản lý lộ trình học tập thông minh
- ✅ Quản lý nội dung (blog, file)
- ✅ Hệ thống phân quyền linh hoạt theo vai trò

### Yêu cầu hệ thống
- Trình duyệt hiện đại (Chrome, Firefox, Edge, Safari)
- Kết nối Internet ổn định
- Tài khoản có quyền Admin hoặc Staff

---

## 🔐 Đăng nhập

### Bước 1: Truy cập trang đăng nhập
Mở trình duyệt và truy cập: `https://[domain]/login`

### Bước 2: Nhập thông tin đăng nhập
```
Email: admin@techhub.com
Password: admin123
```

### Bước 3: Xác thực
- Hệ thống sẽ xác thực thông tin đăng nhập
- Token được lưu tự động trong localStorage
- Tự động refresh token khi hết hạn

### Lưu ý quan trọng
⚠️ **Token Management:**
- Access Token có thời gian sống giới hạn
- Refresh Token được sử dụng để gia hạn phiên
- Khi token hết hạn (401), hệ thống tự động:
  1. Thử refresh token
  2. Nếu thất bại, chuyển về trang đăng nhập
  3. Xóa token trong localStorage

---

## 👤 Menu Avatar - Các Nút Quản Lý

### 📍 Vị trí
Menu Avatar nằm ở **góc trên bên phải** của giao diện quản trị, bên cạnh:
- 🌐 **Switch Language** - Chuyển đổi ngôn ngữ (Tiếng Việt, English, 日本語)
- 🌙 **Dark Mode Toggle** - Bật/tắt chế độ tối
- 🔔 **Notification Bell** - Thông báo hệ thống

### 🖼️ Thông tin hiển thị
Khi click vào avatar, dropdown menu sẽ hiển thị:

**Phần header (thông tin tài khoản):**
- 👤 **Username**: Tên người dùng đang đăng nhập
- 📧 **Email**: Địa chỉ email của tài khoản
- 🏷️ **Role Badge**: Hiển thị vai trò (ADMIN, STAFF, USER, INSTRUCTOR) với badge màu xanh

---

### 🔘 Các Nút Bấm Dẫn Đến Trang Quản Lý

#### 1. **Dashboard** 📊
```
Icon: BarChart3
Hiển thị cho: Chỉ tài khoản ADMIN
Đường dẫn: /manage/accounts
```

**Mục đích:**
- Truy cập nhanh vào trang quản lý tài khoản nhân viên
- Shortcut để quản lý accounts từ bất kỳ trang nào

**Khi nào sử dụng:**
- Cần kiểm tra danh sách nhân viên nhanh chóng
- Muốn thêm/sửa/xóa tài khoản
- Từ trang khác muốn quay về quản lý người dùng

**Lưu ý:** 
- ⚠️ Nút này **chỉ hiển thị cho Admin**, các vai trò khác không thấy

---

#### 2. **Profile** 👤
```
Icon: User
Hiển thị cho: Tất cả người dùng
Đường dẫn: /profile
```

**Chức năng:**
Xem và chỉnh sửa thông tin cá nhân của tài khoản đang đăng nhập

**Các thao tác có thể thực hiện:**
- ✅ Cập nhật thông tin cá nhân (tên, số điện thoại, bio)
- ✅ Đổi mật khẩu
- ✅ Thay đổi avatar/ảnh đại diện
- ✅ Cập nhật thông tin liên hệ
- ✅ Thêm liên kết mạng xã hội (LinkedIn, GitHub, Twitter)

**Hướng dẫn sử dụng:**
1. Click vào Avatar → chọn "Profile"
2. Trang profile hiển thị thông tin hiện tại
3. Click nút "Edit Profile" để chỉnh sửa
4. Cập nhật các trường cần thiết
5. Click "Save Changes" để lưu

**Đổi mật khẩu:**
1. Trong trang Profile, tìm phần "Security"
2. Click "Change Password"
3. Nhập mật khẩu hiện tại
4. Nhập mật khẩu mới (tối thiểu 8 ký tự)
5. Xác nhận mật khẩu mới
6. Click "Update Password"

---

#### 3. **My Learning** 📚
```
Icon: BookText
Hiển thị cho: Tất cả người dùng
Đường dẫn: /my-learning
```

**Chức năng:**
Xem tất cả các khóa học mà bạn đang học hoặc đã hoàn thành

**Thông tin hiển thị:**
- 📖 Danh sách khóa học đã đăng ký
- 📊 Tiến độ học tập (progress bar %)
- ▶️ Bài học tiếp theo cần hoàn thành
- 🏆 Chứng chỉ đã đạt được
- ⏱️ Thời gian học tập gần nhất
- 📈 Thống kê học tập (giờ học, bài hoàn thành)

**Sử dụng:**
- Tiếp tục học khóa học đang dở dang
- Xem lại bài học đã hoàn thành
- Download chứng chỉ khi hoàn thành khóa học
- Review và đánh giá khóa học
- Theo dõi lộ trình học tập cá nhân

---

#### 4. **Settings** ⚙️
```
Icon: Settings
Hiển thị cho: Tất cả người dùng
Đường dẫn: /manage/setting
```

**Chức năng:**
Cấu hình các cài đặt hệ thống và tùy chọn cá nhân

**Các tab cài đặt:**

**a) General (Chung)**
- 🌐 Ngôn ngữ hiển thị: Tiếng Việt, English, 日本語
- 🎨 Theme: Light, Dark, System (theo hệ thống)
- ⏰ Timezone: Múi giờ
- 📅 Date format: Định dạng ngày tháng

**b) Notifications (Thông báo)**
```
Cấu hình nhận thông báo qua các kênh:

📧 Email notifications:
  ☑ Khóa học mới được xuất bản
  ☑ Comment trên nội dung của tôi
  ☑ Cập nhật khóa học đang học
  ☑ Thông báo hệ thống quan trọng
  
🔔 Push notifications:
  ☑ Tin nhắn mới
  ☑ Phân công công việc
  ☑ Cảnh báo khẩn cấp
  
📱 In-app notifications:
  ☑ Tất cả thông báo trong ứng dụng
```

**c) Privacy (Quyền riêng tư)**
- 👁️ Profile visibility: Public/Private
- 🟢 Activity status: Hiển thị trạng thái online
- 📊 Who can see my learning progress
- 💬 Who can contact me

**d) Preferences (Tùy chọn)**
- 📝 Default editor mode
- 💾 Auto-save interval (thời gian tự động lưu)
- 📌 Sidebar position: Left/Right
- 📄 Items per page: Số items hiển thị mỗi trang

---

#### 5. **Logout** 🚪
```
Icon: LogOut (màu đỏ - cảnh báo)
Hiển thị cho: Tất cả người dùng
Chức năng: Đăng xuất khỏi hệ thống
```

**⚠️ Hành động quan trọng**

**Quy trình logout:**
1. Click vào nút "Logout" (màu đỏ)
2. Hệ thống tự động thực hiện các bước:
   - 🔐 Gọi API `/api/auth/logout` để vô hiệu hóa token trên server
   - 🗑️ Xóa `accessToken` từ localStorage
   - 🗑️ Xóa `refreshToken` từ localStorage
   - 🗑️ Xóa `userInfo` từ localStorage
   - 🧹 Clear context (isAuth, role, permissions)
3. ✅ Hiển thị toast notification: "Đăng xuất thành công"
4. ↪️ Redirect về trang chủ `/`

**Lưu ý quan trọng:**
- ⚠️ **Nếu API logout thất bại** (network error), hệ thống vẫn xóa dữ liệu local và logout
- 💾 **Lưu công việc trước khi logout** để tránh mất dữ liệu chưa lưu
- 🔒 **Luôn logout khi rời khỏi máy tính công cộng** để bảo mật tài khoản
- ⏱️ Nếu nút "Logout" bị disabled và hiển thị "Đang đăng xuất...", đợi vài giây

**Xử lý khi logout thất bại:**
```javascript
// Nếu logout bị stuck, mở Console (F12) và chạy:
localStorage.clear();
location.href = '/';
```

---

### 📝 Tóm tắt các nút bấm

| Nút | Icon | Hiển thị cho | Đường dẫn | Mục đích chính |
|-----|------|--------------|-----------|----------------|
| Dashboard | 📊 | Chỉ Admin | `/manage/accounts` | Quản lý tài khoản |
| Profile | 👤 | Tất cả | `/profile` | Thông tin cá nhân |
| My Learning | 📚 | Tất cả | `/my-learning` | Khóa học đang học |
| Settings | ⚙️ | Tất cả | `/manage/setting` | Cài đặt hệ thống |
| Logout | 🚪 | Tất cả | - | Đăng xuất |

---

### 💡 Tips sử dụng Menu Avatar

**1. Kiểm tra vai trò nhanh:**
- Click vào avatar để xem badge vai trò của bạn
- Nếu không thấy nút Dashboard → bạn không phải Admin
- Liên hệ Admin nếu cần thêm quyền

**2. Truy cập nhanh:**
- Sử dụng avatar menu thay vì navigate qua sidebar
- Nhanh hơn khi cần đến Profile hoặc Settings
- Bookmark các trang thường dùng

**3. Bảo mật tài khoản:**
```
Checklist bảo mật:
□ Đã đổi mật khẩu mặc định
□ Mật khẩu đủ mạnh (8+ ký tự, chữ hoa, số, ký tự đặc biệt)
□ Avatar chuyên nghiệp (200x200px, JPG/PNG, max 2MB)
□ Email khôi phục đã xác thực
□ Logout sau khi hoàn thành công việc
□ Không save password trên máy công cộng
```

**4. Workflow khuyến nghị:**

**Khi bắt đầu làm việc:**
1. Login vào hệ thống
2. Click avatar để kiểm tra vai trò và thông tin
3. Kiểm tra notifications (icon chuông 🔔)
4. Vào Dashboard hoặc trang cần làm việc

**Trong quá trình làm việc:**
- Thường xuyên check notifications
- Sử dụng Settings → Auto-save ON để tránh mất dữ liệu
- Sử dụng My Learning để track tiến độ nếu bạn cũng là học viên

**Khi kết thúc:**
1. Lưu toàn bộ công việc đang làm
2. Check notifications cuối cùng
3. Logout an toàn

---

### 🔧 Xử lý sự cố Menu Avatar

#### Vấn đề 1: Avatar không hiển thị
**Triệu chứng:**
- Chỉ thấy icon user placeholder
- Avatar bị broken image

**Nguyên nhân:**
- Ảnh avatar bị lỗi hoặc link broken
- Chưa upload avatar

**Giải pháp:**
- Hệ thống tự động hiển thị fallback (2 chữ cái đầu của username)
- Vào Profile → Upload avatar mới
- Đảm bảo file ảnh đúng định dạng (JPG, PNG)
- Kích thước không quá 2MB

---

#### Vấn đề 2: Thông tin không cập nhật
**Triệu chứng:**
- Username/email cũ vẫn hiển thị sau khi đổi
- Role badge không đúng

**Nguyên nhân:**
- Browser cache
- LocalStorage chưa sync

**Giải pháp:**
1. Hard refresh: `Ctrl + F5` (Windows) hoặc `Cmd + Shift + R` (Mac)
2. Clear browser cache
3. Logout → Login lại
4. Kiểm tra localStorage trong DevTools (F12):
   ```javascript
   // Xem userInfo trong localStorage
   console.log(JSON.parse(localStorage.getItem('userInfo')));
   ```

---

#### Vấn đề 3: Menu không mở được
**Triệu chứng:**
- Click vào avatar không có phản hồi
- Dropdown không xuất hiện

**Giải pháp:**
- Click vào vùng trống khác rồi thử lại
- Refresh trang (F5)
- Kiểm tra JavaScript errors trong Console (F12)
- Thử browser khác
- Disable các extension có thể conflict

---

#### Vấn đề 4: Logout không thành công
**Triệu chứng:**
- Nút "Logout" bị disabled
- Hiển thị "Đang đăng xuất..." quá lâu (>10s)
- Không redirect về trang login

**Giải pháp:**

**Bước 1:** Đợi 5-10 giây

**Bước 2:** Nếu vẫn bị, refresh trang:
```
Ctrl + F5 (Windows)
Cmd + Shift + R (Mac)
```

**Bước 3:** Xóa localStorage thủ công:
```javascript
// Mở DevTools Console (F12), chạy:
localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
localStorage.removeItem('userInfo');
location.href = '/';
```

**Bước 4:** Nếu vẫn không được, clear toàn bộ site data:
- Chrome: Settings → Privacy → Clear browsing data → Cookies and site data
- Firefox: Options → Privacy → Clear Data

---

#### Vấn đề 5: Không thấy nút Dashboard (Admin)
**Triệu chứng:**
- Tài khoản Admin nhưng không thấy nút Dashboard

**Nguyên nhân:**
- Role chưa được cập nhật trong context
- LocalStorage lưu thông tin cũ

**Giải pháp:**
1. Kiểm tra role trong localStorage:
   ```javascript
   // Mở Console (F12)
   const userInfo = JSON.parse(localStorage.getItem('userInfo'));
   console.log('Current role:', userInfo?.roles);
   ```
2. Nếu role không đúng:
   - Logout hoàn toàn
   - Clear localStorage
   - Login lại
3. Nếu vẫn không đúng:
   - Liên hệ Admin khác để kiểm tra role trong database
   - Có thể role đã bị thay đổi

---

## 📊 Dashboard

### Tính năng chính
Dashboard cung cấp tổng quan về hệ thống:

#### 1. **Thống kê Vector Database (Qdrant)**
- Hiển thị số lượng points trong database
- Thống kê collections
- Trạng thái hoạt động của Qdrant

#### 2. **Quản lý AI Learning Path Drafts**
- Xem danh sách các bản nháp lộ trình học tập do AI tạo
- Phê duyệt hoặc từ chối các bản nháp
- Xem chi tiết từng bản nháp

#### 3. **Reindex Dữ liệu**
Các nút reindex để cập nhật vector database:

**a) Reindex Courses**
```
Chức năng: Đồng bộ lại toàn bộ khóa học vào vector database
Khi sử dụng: Sau khi thêm/sửa nhiều khóa học
```

**b) Reindex Lessons**
```
Chức năng: Đồng bộ lại toàn bộ bài học vào vector database
Khi sử dụng: Sau khi thêm/sửa nhiều bài học
```

**c) Reindex All**
```
Chức năng: Đồng bộ lại toàn bộ dữ liệu
Khi sử dụng: Khi cần làm mới hoàn toàn vector database
⚠️ Lưu ý: Quá trình này có thể mất thời gian
```

### Cách sử dụng
1. Truy cập: `/manage/dashboard`
2. Xem thống kê tổng quan
3. Click vào các nút reindex khi cần thiết
4. Theo dõi trạng thái xử lý

---

## 👥 Quản lý Nhân viên

### Đường dẫn
`/manage/accounts`

### Quyền yêu cầu
- Method: `GET`
- Endpoint: `/api/users`

### Tính năng

#### 1. **Xem danh sách nhân viên**
- Bảng hiển thị thông tin: ID, Tên, Email, Vai trò, Trạng thái
- Phân trang tự động
- Tìm kiếm theo tên/email
- Lọc theo vai trò

#### 2. **Thêm nhân viên mới**
**Các bước:**
1. Click nút "Thêm nhân viên"
2. Điền form:
   - **Họ và tên**: Tên đầy đủ của nhân viên
   - **Email**: Email duy nhất (sử dụng để đăng nhập)
   - **Mật khẩu**: Mật khẩu ban đầu
   - **Vai trò**: Chọn vai trò (Admin/Staff/User)
   - **Trạng thái**: Active/Inactive
3. Click "Lưu"

#### 3. **Chỉnh sửa thông tin nhân viên**
**Các bước:**
1. Click icon "Edit" ở hàng nhân viên cần sửa
2. Cập nhật thông tin cần thiết
3. Click "Cập nhật"

**Có thể chỉnh sửa:**
- ✅ Họ tên
- ✅ Vai trò
- ✅ Trạng thái
- ⚠️ Email (cẩn thận khi thay đổi)

#### 4. **Xóa nhân viên**
⚠️ **Cảnh báo**: Hành động này có thể không thể hoàn tác
1. Click icon "Delete"
2. Xác nhận trong dialog
3. Nhân viên sẽ bị xóa hoặc deactivated

### Best Practices
- 📌 Luôn kiểm tra email trước khi thêm mới
- 📌 Sử dụng trạng thái Inactive thay vì xóa khi tạm ngưng
- 📌 Thường xuyên review danh sách nhân viên

---

## 🎭 Quản lý Vai trò

### Đường dẫn
`/manage/roles`

### Quyền yêu cầu
- Method: `GET`
- Endpoint: `/api/admin/roles`

### Tính năng

#### 1. **Xem danh sách vai trò**
Hiển thị tất cả vai trò trong hệ thống:
- **Admin**: Toàn quyền quản trị
- **Staff**: Nhân viên quản lý
- **User**: Người dùng thông thường
- **Instructor**: Giảng viên

#### 2. **Thêm vai trò mới**
**Các bước:**
1. Click "Thêm vai trò"
2. Nhập thông tin:
   - **Tên vai trò**: Tên duy nhất (VD: "Content Manager")
   - **Mô tả**: Mô tả chức năng vai trò
   - **Quyền hạn**: Chọn các quyền từ danh sách
3. Click "Tạo vai trò"

---

## 📞 Hỗ trợ

### Liên hệ khi cần hỗ trợ:
- 📧 Email: support@techhub.com
- 💬 Slack: #techhub-support
- 📱 Hotline: [số điện thoại]

### Trước khi liên hệ, chuẩn bị:
- ✅ Mô tả chi tiết vấn đề
- ✅ Screenshot lỗi
- ✅ Thao tác đã thực hiện
- ✅ Browser và OS đang dùng

---

**Chúc bạn quản trị hiệu quả! 🚀**

*Tài liệu này được cập nhật thường xuyên. Vui lòng check version mới nhất.*
*Phiên bản: 1.0 - Cập nhật: 2025-01-29*
