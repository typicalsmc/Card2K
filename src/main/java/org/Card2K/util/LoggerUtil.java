package org.Card2K.util;

import org.Card2K.NapThePlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LoggerUtil {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public static void logSuccess(String player, String telco, String serial, String code, int amount) {
        NapThePlugin plugin = NapThePlugin.getInstance();
        long timestamp = Instant.now().toEpochMilli();

        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        String timeStr = FORMAT.format(now);
        String monthStr = MONTH_FORMAT.format(now);

        // 1. Ghi log chi tiết theo tháng
        try {
            File monthLog = new File(plugin.getDataFolder(), "log_success_" + monthStr + ".txt");
            monthLog.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(monthLog, true), StandardCharsets.UTF_8))) {
                String detail = String.format("[%s] %s|%s|%d|%s|%s",
                        timeStr, player, telco, amount, serial, code);
                writer.write(detail);
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Không thể ghi file log_success_" + monthStr + ".txt: " + e.getMessage());
        }

        // 2. Cập nhật log_total.txt
        updateTotalLog(plugin, player, amount, monthStr);

        // 3. Reload cache và log thông báo
        plugin.getCardDataCache().reload();
        plugin.getLogger().info("[LOG] ✅ Ghi log nạp: " + player + " - " + amount + "đ");
    }

    private static void updateTotalLog(NapThePlugin plugin, String player, int amount, String monthStr) {
        File totalFile = new File(plugin.getDataFolder(), "log_total.txt");
        int year = Year.now().getValue();

        Map<String, Long> totalMap = new LinkedHashMap<>();
        Map<String, Map<Integer, Long>> yearlyMap = new HashMap<>();
        Map<String, Map<String, Long>> monthlyMap = new HashMap<>();

        // Đọc file cũ
        if (totalFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(totalFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length < 3) continue;
                    String p = parts[0];
                    long tot = Long.parseLong(parts[1]);
                    totalMap.put(p, tot);

                    Map<Integer, Long> yMap = new HashMap<>();
                    Map<String, Long> mMap = new HashMap<>();
                    for (String stat : parts[2].split(";")) {
                        String[] kv = stat.split(":");
                        if (kv.length == 2) {
                            if (kv[0].contains("-")) mMap.put(kv[0], Long.parseLong(kv[1]));
                            else yMap.put(Integer.parseInt(kv[0]), Long.parseLong(kv[1]));
                        }
                    }
                    yearlyMap.put(p, yMap);
                    monthlyMap.put(p, mMap);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("⚠ Không thể đọc log_total.txt: " + e.getMessage());
            }
        }

        // Cập nhật cho player hiện tại
        totalMap.put(player, totalMap.getOrDefault(player, 0L) + amount);
        Map<Integer, Long> yMap = yearlyMap.computeIfAbsent(player, k -> new HashMap<>());
        Map<String, Long> mMap = monthlyMap.computeIfAbsent(player, k -> new HashMap<>());

        yMap.put(year, yMap.getOrDefault(year, 0L) + amount);
        mMap.put(monthStr, mMap.getOrDefault(monthStr, 0L) + amount);

        // Ghi lại toàn bộ
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(totalFile, false), StandardCharsets.UTF_8))) {
            for (String p : totalMap.keySet()) {
                long tot = totalMap.get(p);
                List<String> stats = new ArrayList<>();
                yearlyMap.getOrDefault(p, Collections.emptyMap())
                        .forEach((yr, v) -> stats.add(yr + ":" + v));
                monthlyMap.getOrDefault(p, Collections.emptyMap())
                        .forEach((mo, v) -> stats.add(mo + ":" + v));
                writer.write(p + "|" + tot + "|" + String.join(";", stats));
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Không thể ghi file log_total.txt: " + e.getMessage());
        }
    }

    // Ghi log chi tiết cũ (nếu còn dùng)
    public static void log(String player, String telco, String serial,
                           String code, int amount, int value, int status, String message) {
        try {
            File file = new File(JavaPlugin.getProvidingPlugin(LoggerUtil.class)
                    .getDataFolder(), "log_success.txt");
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                String time = FORMAT.format(LocalDateTime.now());
                String line = String.format(
                        "[%s] | Player: %s | Telco: %s | Serial: %s | Code: %s | Amount: %d | Value: %d | Status: %d | Message: %s",
                        time, player, telco, serial, code, amount, value, status, message);
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            NapThePlugin.getInstance().getLogger()
                    .severe("Không thể ghi log chi tiết nạp thẻ: " + e.getMessage());
        }
    }
}
