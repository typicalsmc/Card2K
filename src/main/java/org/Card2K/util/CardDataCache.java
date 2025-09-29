package org.Card2K.util;

import org.Card2K.NapThePlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CardDataCache {

    private final NapThePlugin plugin;
    private final File totalLogFile;

    private final DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy");

    private String currentMonth;
    private String currentYear;

    private final Map<String, Integer> totalMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> monthMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> yearMap = new ConcurrentHashMap<>();

    public CardDataCache(NapThePlugin plugin) {
        this.plugin = plugin;
        this.totalLogFile = new File(plugin.getDataFolder(), "log_total.txt");
        reload();
    }

    public void reload() {
        totalMap.clear();
        monthMap.clear();
        yearMap.clear();

        LocalDate now = LocalDate.now();
        currentMonth = now.format(monthFmt);
        currentYear = now.format(yearFmt);

        if (totalLogFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(totalLogFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length < 3) continue;

                    String player = parts[0].trim().toLowerCase();
                    int total;
                    try {
                        total = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    totalMap.put(player, total);

                    String[] detailParts = parts[2].split(";");
                    for (String d : detailParts) {
                        String[] kv = d.split(":");
                        if (kv.length != 2) continue;
                        String key = kv[0];
                        int value;
                        try {
                            value = Integer.parseInt(kv[1]);
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        if (key.equals(currentYear)) {
                            yearMap.merge(player, value, Integer::sum);
                        }
                        if (key.equals(currentMonth)) {
                            monthMap.merge(player, value, Integer::sum);
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Lỗi đọc log_total.txt: " + e.getMessage());
            }
        }
    }


    public int getTotal(String player) {
        return totalMap.getOrDefault(player.toLowerCase(), 0);
    }

    public int getTotalMonth(String player) {
        return monthMap.getOrDefault(player.toLowerCase(), 0);
    }

    public int getTotalYear(String player) {
        return yearMap.getOrDefault(player.toLowerCase(), 0);
    }

    public List<Map.Entry<String, Integer>> getTopTotal(int limit) {
        return totalMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> getTopMonth(int limit) {
        return monthMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> getTopYear(int limit) {
        return yearMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
