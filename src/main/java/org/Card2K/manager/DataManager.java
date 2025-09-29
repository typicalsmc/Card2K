package org.Card2K.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.Card2K.NapThePlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final NapThePlugin plugin;
    private final File logFile;

    private final Map<String, Integer> playerTotal = new ConcurrentHashMap<>();
    private final Set<String> rewardedMilestoneKeys = ConcurrentHashMap.newKeySet();

    public DataManager(NapThePlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "log_total.txt");

        createLogFileIfNotExists();
        loadTotals();
    }

    public void reload() {
        playerTotal.clear();
        rewardedMilestoneKeys.clear();
        loadTotals();
    }

    private void createLogFileIfNotExists() {
        if (!logFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Không thể tạo file log_total.txt: " + e.getMessage());
            }
        }
    }

    private void loadTotals() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("|")) continue;

                String[] parts = line.split("\\|");
                if (parts.length < 2) continue;

                String player = parts[0].trim().toLowerCase();
                try {
                    int amount = Integer.parseInt(parts[1].trim());
                    playerTotal.put(player, playerTotal.getOrDefault(player, 0) + amount);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[DEBUG] Không thể parse số tiền từ dòng: " + line);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không đọc được file log_total.txt: " + e.getMessage());
        }
    }

    public void addPlayerAmount(String playerName, int amount) {
        String key = playerName.toLowerCase();
        playerTotal.put(key, playerTotal.getOrDefault(key, 0) + amount);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
            long timestamp = System.currentTimeMillis();
            writer.write(key + "|" + amount + "|" + timestamp);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Không ghi được log vào log_total.txt: " + e.getMessage());
        }
    }

    public int getTotal(String playerName) {
        String key = playerName.toLowerCase();
        int total = playerTotal.getOrDefault(key, 0);
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] getTotal(" + key + ") = " + total);
        }
        return total;
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
            } else if (cmd.startsWith("player:")) {
                player.performCommand(real.substring("player:".length()));
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
            }
        }
    }


    public Map<String, Integer> readAllLogs() {
        return new ConcurrentHashMap<>(playerTotal);
    }
}
