package org.Card2K.util;

import org.Card2K.NapThePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CardDataManager {

    private final NapThePlugin plugin;
    private final File logDir;
    private final File milestoneFile;

    private final Map<String, Integer> totalMap = new HashMap<>();
    private final Map<String, Integer> todayMap = new HashMap<>();
    private final Map<String, Integer> monthMap = new HashMap<>();
    private final Map<String, Integer> yearMap = new HashMap<>();

    private final Map<String, Set<Integer>> playerMilestones = new HashMap<>();

    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    private LocalDate currentDate;
    private String currentMonth;
    private String currentYear;

    public CardDataManager(NapThePlugin plugin) {
        this.plugin = plugin;
        this.logDir = plugin.getDataFolder();
        this.milestoneFile = new File(plugin.getDataFolder(), "milestone_done.yml");
        reload();
    }

    public synchronized void reload() {
        totalMap.clear();
        todayMap.clear();
        monthMap.clear();
        yearMap.clear();
        playerMilestones.clear();

        currentDate = LocalDate.now();
        currentMonth = currentDate.format(monthFormatter);
        currentYear = currentDate.format(yearFormatter);

        loadLog(new File(logDir, "log_total.txt"));
        loadLog(new File(logDir, "log_success_" + currentMonth + ".txt"));

        loadDoneMilestones();
    }

    private void loadLog(File file) {
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("|")) continue;

                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                String player = parts[0].trim().toLowerCase();
                int amount;
                long timestamp;
                try {
                    amount = Integer.parseInt(parts[1].trim());
                    timestamp = Long.parseLong(parts[2].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                Instant instant = Instant.ofEpochMilli(timestamp);
                ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
                LocalDate date = dateTime.toLocalDate();

                totalMap.put(player, totalMap.getOrDefault(player, 0) + amount);
                if (date.format(dayFormatter).equals(currentDate.format(dayFormatter))) {
                    todayMap.put(player, todayMap.getOrDefault(player, 0) + amount);
                }
                if (date.format(monthFormatter).equals(currentMonth)) {
                    monthMap.put(player, monthMap.getOrDefault(player, 0) + amount);
                }
                if (date.format(yearFormatter).equals(currentYear)) {
                    yearMap.put(player, yearMap.getOrDefault(player, 0) + amount);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không đọc được file log: " + file.getName() + ": " + e.getMessage());
        }
    }

    public synchronized void addPlayerAmount(String playerName, int amount) {
        String key = playerName.toLowerCase();
        totalMap.put(key, totalMap.getOrDefault(key, 0) + amount);
        long timestamp = System.currentTimeMillis();

        File totalLog = new File(logDir, "log_total.txt");
        File monthLog = new File(logDir, "log_success_" + currentMonth + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(totalLog, true), StandardCharsets.UTF_8))) {
            writer.write(key + "|" + amount + "|" + timestamp);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Không ghi được log_total.txt: " + e.getMessage());
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(monthLog, true), StandardCharsets.UTF_8))) {
            writer.write(key + "|" + amount + "|" + timestamp);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Không ghi được log tháng: " + e.getMessage());
        }
    }

    public synchronized int getTotal(String playerName) {
        return totalMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public synchronized int getTotalToday(String playerName) {
        return todayMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public synchronized int getTotalMonth(String playerName) {
        return monthMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public synchronized int getTotalYear(String playerName) {
        return yearMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public synchronized List<Map.Entry<String, Integer>> getTopToday(int limit) {
        return todayMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit).collect(Collectors.toList());
    }

    public synchronized List<Map.Entry<String, Integer>> getTopMonth(int limit) {
        return monthMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit).collect(Collectors.toList());
    }

    public synchronized List<Map.Entry<String, Integer>> getTopYear(int limit) {
        return yearMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit).collect(Collectors.toList());
    }

    public synchronized List<Map.Entry<String, Integer>> getTopTotal(int limit) {
        return totalMap.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit).collect(Collectors.toList());
    }

    private void loadDoneMilestones() {
        if (!milestoneFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(milestoneFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(":")) continue;
                String[] parts = line.split(":");
                if (parts.length < 2) continue;
                String player = parts[0].trim().toLowerCase();
                String[] milestones = parts[1].split(",");
                Set<Integer> doneSet = new HashSet<>();
                for (String m : milestones) {
                    try {
                        doneSet.add(Integer.parseInt(m.trim()));
                    } catch (NumberFormatException ignored) {}
                }
                playerMilestones.put(player, doneSet);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không đọc được milestone_done.yml: " + e.getMessage());
        }
    }

    public synchronized Set<Integer> getDoneMilestones(String playerName) {
        return playerMilestones.getOrDefault(playerName.toLowerCase(), new HashSet<>());
    }

    public synchronized void saveDoneMilestones(String playerName, Set<Integer> doneMilestones) {
        playerMilestones.put(playerName.toLowerCase(), doneMilestones);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(milestoneFile))) {
            for (Map.Entry<String, Set<Integer>> entry : playerMilestones.entrySet()) {
                String line = entry.getKey() + ":" + String.join(",", entry.getValue().stream().map(String::valueOf).toArray(String[]::new));
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không lưu được milestone_done.yml: " + e.getMessage());
        }
    }

    public synchronized void checkMilestones(String playerName, Player player) {
        if (!plugin.getConfig().getBoolean("milestones.enable", false)) {
            return;
        }

        int total = getTotal(playerName.toLowerCase());
        Map<Integer, List<String>> milestones = plugin.getConfig().getConfigurationSection("milestones.command") != null ? parseMilestones() : new HashMap<>();

        Set<Integer> done = playerMilestones.computeIfAbsent(playerName.toLowerCase(), k -> new HashSet<>());

        for (Map.Entry<Integer, List<String>> entry : milestones.entrySet()) {
            int threshold = entry.getKey();
            if (total >= threshold && !done.contains(threshold)) {
                executeCommands(entry.getValue(), player);
                done.add(threshold);
                saveDoneMilestones(playerName, done);
                player.sendMessage("§aChúc mừng bạn đã đạt mốc nạp §e" + threshold + "đ§a!");
            }
        }
    }

    private Map<Integer, List<String>> parseMilestones() {
        Map<Integer, List<String>> map = new TreeMap<>();
        for (String key : plugin.getConfig().getConfigurationSection("milestones.command").getKeys(false)) {
            try {
                int amount = Integer.parseInt(key);
                List<String> cmds = plugin.getConfig().getStringList("milestones.command." + key);
                map.put(amount, cmds);
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private void executeCommands(List<String> commands, Player player) {
        for (String cmd : commands) {
            String real = cmd.replace("{player}", player.getName());
            if (cmd.startsWith("console:")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real.substring("console:".length()));
            } else if (cmd.startsWith("op:")) {
                boolean wasOp = player.isOp();
                player.setOp(true);
                player.performCommand(real.substring("op:".length()));
                player.setOp(wasOp);
            } else {
                player.performCommand(real);
            }
        }
    }

    public CardDataCache getCache() {
        return plugin.getCardDataCache();
    }
}
