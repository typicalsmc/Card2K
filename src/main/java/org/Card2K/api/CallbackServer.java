package org.Card2K.api;

import org.Card2K.NapThePlugin;
import org.Card2K.util.CommandExecutor;
import org.Card2K.util.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CallbackServer {

    private static final Map<String, String> pendingRequests = new ConcurrentHashMap<>();

    public static void addPendingRequest(String requestId, String playerName, String telco, String code, String serial, int amount) {
        pendingRequests.put(requestId, playerName);
        // Schedule first check after 5 minutes (run only once by using very large period)
        NapThePlugin.getInstance().getFoliaLib().getScheduler().runTimerAsync(() -> {
            checkStatus(requestId, playerName, telco, code, serial, amount, false);
        }, 20L * 60 * 5, Long.MAX_VALUE);

        // Schedule second check after 10 minutes (run only once by using very large period)
        NapThePlugin.getInstance().getFoliaLib().getScheduler().runTimerAsync(() -> {
            checkStatus(requestId, playerName, telco, code, serial, amount, true);
        }, 20L * 60 * 10, Long.MAX_VALUE);
    }

    private static void checkStatus(String requestId, String playerName, String telco, String code, String serial, int amount, boolean finalTry) {
        try {
            String domain = "http://card2k.com";
            String partnerId = NapThePlugin.getInstance().getConfig().getString("Card2kAPI.key");
            String partnerKey = NapThePlugin.getInstance().getConfig().getString("Card2kAPI.secret");
            String sign = org.Card2K.util.MD5Util.md5(partnerKey + code + serial);

            String data = "telco=" + URLEncoder.encode(telco, "UTF-8") +
                    "&code=" + URLEncoder.encode(code, "UTF-8") +
                    "&serial=" + URLEncoder.encode(serial, "UTF-8") +
                    "&amount=" + amount +
                    "&request_id=" + requestId +
                    "&partner_id=" + partnerId +
                    "&sign=" + sign +
                    "&command=check";

            URL url = new URL(domain + "/chargingws/v2");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            OutputStream os = con.getOutputStream();
            os.write(data.getBytes());
            os.flush();
            os.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            JSONObject json = new JSONObject(sb.toString());
            int status = json.getInt("status");

            if (status == 1 || status == 2) {
                NapThePlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                    Player player = Bukkit.getPlayerExact(playerName);
                    LoggerUtil.logSuccess(playerName, telco, serial, code, amount);

                    if (player != null) {
                        CommandExecutor.executeCommands(player, amount);
                        NapThePlugin.getInstance().getCardDataManager().checkMilestones(playerName, player);
                        player.sendMessage("§aNạp thẻ thành công! Mệnh giá: " + amount + "đ.");
                    } else {
                        Bukkit.getLogger().info("⚠ Người chơi " + playerName + " offline, đã cộng tổng nạp.");
                    }
                });
            } else if (status == 99 && finalTry) {
                Bukkit.getLogger().warning("⚠ Thẻ vẫn chưa xử lý sau 10 phút: " + requestId);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("⚠ Lỗi khi kiểm tra thẻ: " + e.getMessage());
        }
    }
}
