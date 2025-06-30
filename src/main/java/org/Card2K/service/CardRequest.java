package org.Card2K.service;

import org.Card2K.NapThePlugin;
import org.Card2K.util.MD5Util;
import org.Card2K.util.LoggerUtil;
import org.Card2K.util.CommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class CardRequest {

    public static void requestCard(NapThePlugin plugin, Player player, String telco, String code, String serial, int amount, String requestId) {
        FileConfiguration config = plugin.getConfig();
        String partnerId = config.getString("Card2kAPI.key");
        String partnerKey = config.getString("Card2kAPI.secret");

        String sign = MD5Util.md5(partnerKey + code + serial);
        String endpoint = "http://card2k.com/chargingws/v2";

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String data = "telco=" + URLEncoder.encode(telco, "UTF-8")
                            + "&code=" + URLEncoder.encode(code, "UTF-8")
                            + "&serial=" + URLEncoder.encode(serial, "UTF-8")
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
                        os.write(data.getBytes());
                        os.flush();
                    }

                    StringBuilder response = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject json = new JSONObject(response.toString());
                    int status = json.getInt("status");
                    String message = json.getString("message");

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (status == 99) {
                            player.sendMessage("§aThẻ đã gửi, đang chờ xử lý...");
                            int delayMinutes = config.getInt("delay_before_reward", 5);
                            int maxRetry = config.getInt("max_retry", 1);
                            scheduleCheck(plugin, player.getName(), telco, code, serial, amount, requestId, partnerId, partnerKey, 0, delayMinutes, maxRetry);
                        } else if (status == 1 || status == 2) {
                            player.sendMessage("§aThẻ đã được xử lý ngay!");
                            handleSuccess(player.getName(), telco, serial, code, amount);
                        } else {
                            player.sendMessage("§cThẻ lỗi: " + message);
                        }
                    });

                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage("§cLỗi gửi thẻ: " + e.getMessage())
                    );
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private static void scheduleCheck(NapThePlugin plugin, String playerName, String telco, String code, String serial, int amount, String requestId, String partnerId, String partnerKey, int attempt, int delayMinutes, int maxRetry) {
        long delayTicks = 20L * 60 * delayMinutes;

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                String sign = MD5Util.md5(partnerKey + code + serial);
                String checkData = "telco=" + URLEncoder.encode(telco, "UTF-8")
                        + "&code=" + URLEncoder.encode(code, "UTF-8")
                        + "&serial=" + URLEncoder.encode(serial, "UTF-8")
                        + "&amount=" + amount
                        + "&request_id=" + requestId
                        + "&partner_id=" + partnerId
                        + "&sign=" + sign
                        + "&command=check";

                HttpURLConnection con = (HttpURLConnection) new URL("http://card2k.com/chargingws/v2").openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(checkData.getBytes());
                    os.flush();
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject json = new JSONObject(response.toString());
                int status = json.getInt("status");

                if (status == 1 || status == 2) {
                    int realAmount = json.optInt("value", amount);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayerExact(playerName);
                        handleSuccess(playerName, telco, serial, code, realAmount);
                        if (player != null) {
                            player.sendMessage("§aNạp thẻ thành công sau khi xử lý! Mệnh giá: " + realAmount + "đ.");
                        }
                    });
                } else if (status == 99 && attempt < maxRetry) {
                    // Retry tiếp sau delay cố định
                    scheduleCheck(plugin, playerName, telco, code, serial, amount, requestId, partnerId, partnerKey, attempt + 1, delayMinutes, maxRetry);
                } else {
                    Bukkit.getLogger().warning("❌ Giao dịch chưa xử lý sau " + ((attempt + 1) * delayMinutes) + " phút: request_id=" + requestId);
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning("⚠️ Lỗi kiểm tra trạng thái thẻ: " + e.getMessage());
            }
        }, delayTicks);
    }

    private static void handleSuccess(String playerName, String telco, String serial, String code, int amount) {
        LoggerUtil.logSuccess(playerName, telco, serial, code, amount);

        NapThePlugin.getInstance().getCardDataManager().addPlayerAmount(playerName, amount);

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            CommandExecutor.executeCommands(player, amount);
            NapThePlugin.getInstance().getCardDataManager().checkMilestones(playerName, player);
        } else {
            Bukkit.getLogger().info("✔ Người chơi " + playerName + " offline, đã cộng tổng nạp.");
        }
    }
}
