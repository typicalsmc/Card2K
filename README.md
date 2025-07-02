💳 Card2K Plugin – Hệ thống nạp thẻ cho Minecraft (1.16–1.21.6)

Plugin hỗ trợ nạp thẻ cào qua API Card2K, tích hợp PlaceholderAPI, hỗ trợ giao diện nhập bằng AnvilGUI, chạy tốt trên Paper/Spigot từ 1.16 đến 1.21.6.

✅ Cần có plugin placeholderAPI để sủ dụng :https://www.spigotmc.org/resources/placeholderapi.6245/

✅ Khuyến khích dùng kèm: EssentialsX, EssentialsXChat để hiển thị placeholder tốt hơn (không bắt buộc)

---

🚀 CÀI ĐẶT

1. Tải file .jar và đặt vào thư mục `plugins/` trên server Minecraft.
2. Khởi động lại server hoặc chạy lệnh:

   /napthereload

3. Plugin sẽ tự tạo các file:
   - config.yml
   - log_success.txt
   - milestone_done.yml

4. Nếu khởi động thành công, console sẽ hiện:
   [Card2k] NapThePlugin đã bật thành công!

---

⚙️ CẤU HÌNH (config.yml)

Card2k-API:
  key: 'YOUR_PARTNER_ID'
  secret: 'YOUR_PARTNER_KEY'

delay_before_reward: 2
max_retry: 1
placeholder_update: 300
debug: false

card:
  command:
    10000:
      - "console: give {player} diamond 1"
    50000:
      - "console: give {player} diamond 5"

milestones:
  command:
    100000:
      - "eco give {player} 50000"

📌 `{player}` sẽ được thay bằng tên người chơi thực tế.

---

📜 LỆNH

/napthe <telco> <amount> <seri> <code>   Gửi yêu cầu nạp thẻ  
/napthereload                            Reload plugin và config (yêu cầu quyền card2k.reload)

---

🧩 PLACEHOLDERAPI

Plugin sẽ tự động hook nếu PlaceholderAPI được cài.  
Các placeholder:

%card2k_total%                          Tổng tiền đã nạp (mọi thời gian)  
%card2k_total_month%                    Tổng tiền đã nạp trong tháng  
%card2k_total_year%                     Tổng tiền đã nạp trong năm  
%card2k_top_month_1%                    Top 1 tháng này  
%card2k_top_year_1%                     Top 1 năm nay  
%card2k_top_total_1%                    Top 1 tổng nạp 
%card2k_top_month_1_amount%             Số tiền đã nạp cho top tháng
%card2k_top_year_1_amount%              Số tiền đã nạp cho top năm
%card2k_top_total_1_amount%             Số tiền đã nạp cho top tổng

Ví dụ:
  /papi parse me %card2k_top_month_1%

---

Web nạp thẻ cần gọi GET đến URL trên với các tham số:  
status, declared_value, request_id, code, serial, telco, callback_sign

Khi nhận callback thành công, plugin sẽ:
- Ghi log
- Thưởng tiền hoặc phần quà tương ứng

