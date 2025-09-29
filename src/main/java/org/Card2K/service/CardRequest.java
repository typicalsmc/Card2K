package org.Card2K.service;

import org.Card2K.NapThePlugin;
import org.Card2K.util.MD5Util;
import org.Card2K.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CardRequest {

    public static void requestCard(NapThePlugin plugin, Player player, String telco, String code, String serial, int amount, String requestId) {
        FileConfiguration config = plugin.getConfig();
        String partnerId = config.getString("Card2kAPI.key");
        String partnerKey = config.getString("Card2kAPI.secret");

        String sign = MD5Util.md5(partnerKey + code + serial);
        String endpoint = "https://sandbox.card2k.com/chargingws/v2";

        new Thread(() -> {
            try {
                String data = "telco=" + URLEncoder.encode(telco, StandardCharsets.UTF_8)
                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&serial=" + URLEncoder.encode(serial, StandardCharsets.UTF_8)
                        + "&amount=" + amount
                        + "&request_id=" + requestId
                        + "&partner_id=" + partnerId
                        + "&sign=" + sign
                        + "&command=charging";

                HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = con.getResponseCode();
                InputStream stream = (responseCode >= 200 && responseCode < 300)
                        ? con.getInputStream()
                        : con.getErrorStream();

                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }

                String rawResponse = response.toString();
                plugin.getLogger().info(ChatColor.GREEN + "[Card2K] Charging Response: " + ChatColor.RESET + rawResponse);

                if (responseCode != 200) {
                    plugin.getFoliaLib().getScheduler().runAtEntity(player, task ->
                        player.sendMessage(ChatColor.RED + "✘ Lỗi gửi thẻ: Server trả về mã " + responseCode));
                    return;
                }

                if (!rawResponse.trim().startsWith("{")) {
                    plugin.getFoliaLib().getScheduler().runAtEntity(player, task ->
                        player.sendMessage(ChatColor.RED + "✘ Lỗi gửi thẻ: Phản hồi không hợp lệ từ API."));
                    return;
                }

                JSONObject json = new JSONObject(rawResponse);
                int status = json.optInt("status", -1);

                plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (status == 99 || status == 1 || status == 2) {
                        player.sendMessage(ChatColor.YELLOW + "✔ Thẻ của bạn đã gửi thành công, đang chờ xử lý...");
                        plugin.getLogger().info(ChatColor.AQUA + "[Card2K] Thẻ gửi thành công: request_id=" + requestId);
                        scheduleCheck(plugin, player.getName(), telco, code, serial, amount, requestId, partnerId, partnerKey);
                    } else {
                        player.sendMessage(ChatColor.RED + "✘ Nạp thẻ thất bại: " + json.optString("message", "Không có nội dung") + " (status: " + status + ")");
                        plugin.getLogger().warning("[Card2K] Lỗi nạp thẻ: status=" + status + ", message=" + json.optString("message", "Không có nội dung"));
                    }
                });

            } catch (Exception e) {
                plugin.getFoliaLib().getScheduler().runAtEntity(player, task ->
                    player.sendMessage(ChatColor.RED + "✘ Lỗi gửi thẻ: " + e.getMessage()));
                plugin.getLogger().warning("[Card2K] Lỗi requestCard: " + e.toString());
            }
        }, "Card2K-RequestThread-" + requestId).start();
    }

    private static void scheduleCheck(NapThePlugin plugin, String playerName, String telco, String code, String serial, int amount,
                                      String requestId, String partnerId, String partnerKey) {

        final int checkIntervalSeconds = 10;
        final int maxCheckMinutes = 3;
        final int maxAttempts = (maxCheckMinutes * 60) / checkIntervalSeconds;

        plugin.getFoliaLib().getScheduler().runTimerAsync(new Runnable() {
            int attempts = 0;
            boolean success = false;

            @Override
            public void run() {
                attempts++;
                if (success || attempts > maxAttempts) {
                    if (!success) {
                        plugin.getLogger().info(ChatColor.YELLOW + "[Card2K] Dừng kiểm tra sau " + attempts + " lần (timeout): request_id=" + requestId);
                    }
                    return;
                }

                try {
                    String sign = MD5Util.md5(partnerKey + code + serial);
                    String checkData = "telco=" + URLEncoder.encode(telco, StandardCharsets.UTF_8)
                            + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                            + "&serial=" + URLEncoder.encode(serial, StandardCharsets.UTF_8)
                            + "&amount=" + amount
                            + "&request_id=" + requestId
                            + "&partner_id=" + partnerId
                            + "&sign=" + sign
                            + "&command=check";

                    HttpURLConnection con = (HttpURLConnection) new URL("https://sandbox.card2k.com/chargingws/v2").openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Accept", "application/json");
                    con.setDoOutput(true);

                    try (OutputStream os = con.getOutputStream()) {
                        os.write(checkData.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    int responseCode = con.getResponseCode();
                    InputStream stream = (responseCode >= 200 && responseCode < 300)
                            ? con.getInputStream()
                            : con.getErrorStream();

                    StringBuilder response = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = in.readLine()) != null) response.append(line);
                    }

                    String rawResponse = response.toString();

                    plugin.getLogger().info(ChatColor.GRAY + "[Card2K] Kiểm tra request_id=" + requestId + ", attempt=" + attempts);

                    if (!rawResponse.trim().startsWith("{")) {
                        plugin.getLogger().warning("[Card2K] Check trả về không phải JSON: " + rawResponse);
                        return;
                    }

                    JSONObject json = new JSONObject(rawResponse);
                    int status = json.optInt("status", -1);

                    if (status == 1 || status == 2) {
                        int realAmount = json.optInt("value", amount);
                        success = true;

                        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
                            handleSuccess(playerName, telco, serial, code, realAmount);
                            Player player = Bukkit.getPlayerExact(playerName);
                            if (player != null) {
                                player.sendMessage(ChatColor.GREEN + "✔ Nạp thẻ thành công sau khi xử lý! Mệnh giá: " + realAmount + "đ.");
                            }
                        });
                        success = true;

                    } else if (status == 99) {

                    } else {
                        plugin.getLogger().warning(ChatColor.RED + "[Card2K] Giao dịch thất bại (status=" + status + "): " + json.optString("message", ""));
                        success = true; // Stop checking
                    }

                } catch (Exception e) {
                    plugin.getLogger().warning("[Card2K] Lỗi kiểm tra trạng thái thẻ: " + e.getMessage());
                    success = true; // Stop checking on error
                }
            }
        }, 0L, checkIntervalSeconds * 20L);
    }

    private static void handleSuccess(String playerName, String telco, String serial, String code, int amount) {
        LoggerUtil.logSuccess(playerName, telco, serial, code, amount);
        NapThePlugin.getInstance().getCardDataManager().addPlayerAmount(playerName, amount);

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            List<String> commands = NapThePlugin.getInstance().getConfig().getStringList("card.command." + amount);
            if (commands != null) {
                for (String cmd : commands) {
                    String command = cmd.replace("{player}", playerName);
                    NapThePlugin.getInstance().getFoliaLib().getScheduler().runNextTick(
                            task -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command)
                    );
                }
            }
            NapThePlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> NapThePlugin.getInstance().getCardDataManager().checkMilestones(playerName, player));

        } else {
            NapThePlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task ->
                    Bukkit.getLogger().info(ChatColor.GREEN + "✔ Người chơi " + playerName + " offline, đã cộng tổng nạp.")
            );
        }
    }
}
