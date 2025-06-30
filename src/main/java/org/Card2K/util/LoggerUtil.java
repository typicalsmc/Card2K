package org.Card2K.util;

import org.Card2K.NapThePlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        int year = now.getYear();

        try {
            File monthLog = new File(plugin.getDataFolder(), "log_success_" + monthStr + ".txt");
            monthLog.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(monthLog, true), StandardCharsets.UTF_8))) {
                String detail = String.format("[%s] %s|%s|%d|%s|%s", timeStr, player, telco, amount, serial, code);
                writer.write(detail);
                writer.newLine();
            }

        } catch (IOException e) {
            plugin.getLogger().severe("❌ Không thể ghi file log_success_" + monthStr + ".txt: " + e.getMessage());
        }

        // 2. Ghi log tổng vào log_total.txt (để phục vụ top/mốc)
        File totalFile = new File(plugin.getDataFolder(), "log_total.txt");
        Map<String, Long> totalMap = new HashMap<>();
        Map<String, Map<Integer, Long>> yearlyMap = new HashMap<>();
        Map<String, Map<String, Long>> monthlyMap = new HashMap<>();

        if (totalFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(totalFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        String p = parts[0];
                        long total = Long.parseLong(parts[1]);
                        totalMap.put(p, total);

                        Map<Integer, Long> yearData = new HashMap<>();
                        Map<String, Long> monthData = new HashMap<>();
                        String[] dataParts = parts[2].split(";");

                        for (String d : dataParts) {
                            if (d.contains(":")) {
                                String[] kv = d.split(":");
                                if (kv.length == 2) {
                                    String key = kv[0];
                                    long value = Long.parseLong(kv[1]);

                                    if (key.contains("-")) {
                                        monthData.put(key, value);
                                    } else {
                                        yearData.put(Integer.parseInt(key), value);
                                    }
                                }
                            }
                        }

                        yearlyMap.put(p, yearData);
                        monthlyMap.put(p, monthData);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("⚠ Không thể đọc log_total.txt: " + e.getMessage());
            }
        }

        // Cập nhật
        totalMap.put(player, totalMap.getOrDefault(player, 0L) + amount);
        Map<Integer, Long> playerYearMap = yearlyMap.getOrDefault(player, new HashMap<>());
        Map<String, Long> playerMonthMap = monthlyMap.getOrDefault(player, new HashMap<>());

        playerYearMap.put(year, playerYearMap.getOrDefault(year, 0L) + amount);
        playerMonthMap.put(monthStr, playerMonthMap.getOrDefault(monthStr, 0L) + amount);

        yearlyMap.put(player, playerYearMap);
        monthlyMap.put(player, playerMonthMap);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(totalFile, false), StandardCharsets.UTF_8))) {
            for (String p : totalMap.keySet()) {
                long total = totalMap.get(p);
                Map<Integer, Long> yMap = yearlyMap.getOrDefault(p, new HashMap<>());
                Map<String, Long> mMap = monthlyMap.getOrDefault(p, new HashMap<>());

                List<String> allData = new ArrayList<>();
                for (Map.Entry<Integer, Long> entry : yMap.entrySet()) {
                    allData.add(entry.getKey() + ":" + entry.getValue());
                }
                for (Map.Entry<String, Long> entry : mMap.entrySet()) {
                    allData.add(entry.getKey() + ":" + entry.getValue());
                }

                writer.write(p + "|" + total + "|" + String.join(";", allData));
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Không thể ghi file log_total.txt: " + e.getMessage());
        }

        plugin.getCardDataCache().reload();
        plugin.getLogger().info("[LOG] ✅ Ghi log nạp: " + player + " - " + amount + "đ");
    }

    // Ghi log chi tiết cũ (giữ nguyên để tương thích nếu còn dùng nơi khác)
    public static void log(String player, String telco, String serial, String code, int amount, int value, int status, String message) {
        try {
            File file = new File(JavaPlugin.getProvidingPlugin(LoggerUtil.class).getDataFolder(), "log_success.txt");
            file.getParentFile().mkdirs();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            String time = FORMAT.format(LocalDateTime.now());

            String line = String.format(
                    "[%s] | Player: %s | Telco: %s | Serial: %s | Code: %s | Amount: %d | Value: %d | Status: %d | Message: %s",
                    time, player, telco, serial, code, amount, value, status, message
            );

            writer.write(line);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            NapThePlugin.getInstance().getLogger().severe("Không thể ghi log chi tiết nạp thẻ: " + e.getMessage());
        }
    }
}
