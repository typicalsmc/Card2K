package org.Card2K.util;

import org.Card2K.NapThePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CardDataManager {

    private final NapThePlugin plugin;
    private final File logDir;
    private final File milestoneFile;

    private final Map<String, Integer> totalMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> todayMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> monthMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> yearMap = new ConcurrentHashMap<>();

    private final Map<String, Set<Integer>> playerMilestones = new ConcurrentHashMap<>();

    private final DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy");

    private LocalDate currentDate;
    private String currentMonth;
    private String currentYear;

    public CardDataManager(NapThePlugin plugin) {
        this.plugin = plugin;
        this.logDir = plugin.getDataFolder();
        this.milestoneFile = new File(plugin.getDataFolder(), "milestone_done.yml");
        reload();
    }

    public void reload() {
        totalMap.clear();
        todayMap.clear();
        monthMap.clear();
        yearMap.clear();
        playerMilestones.clear();

        currentDate = LocalDate.now();
        currentMonth = currentDate.format(monthFmt);
        currentYear = currentDate.format(yearFmt);

        loadLog(new File(logDir, "log_total.txt"));
        loadDoneMilestones();
    }

    private void loadLog(File file) {
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length < 3) {
                    continue;
                }
                String player = parts[0].toLowerCase();
                int tot = Integer.parseInt(parts[1]);
                totalMap.put(player, tot);

                Map<String, Integer> stats = new HashMap<>();
                for (String stat : parts[2].split(";")) {
                    String[] kv = stat.split(":");
                    if (kv.length == 2) {
                        stats.put(kv[0], Integer.parseInt(kv[1]));
                    }
                }

                stats.forEach((k, v) -> {
                    if (k.equals(currentYear)) {
                        yearMap.put(player, v);
                    } else if (k.equals(currentMonth)) {
                        monthMap.put(player, v);
                    } else if (k.equals(currentDate.format(dayFmt))) {
                        todayMap.put(player, v);
                    }
                });
            }
        } catch (IOException | NumberFormatException e) {
            plugin.getLogger().warning("Không đọc được file log: " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Chỉ cập nhật map, không ghi file nữa
     */
    public void addPlayerAmount(String playerName, int amount) {
        String key = playerName.toLowerCase();

        totalMap.merge(key, amount, Integer::sum);
        yearMap.merge(key, amount, Integer::sum);
        monthMap.merge(key, amount, Integer::sum);
        todayMap.merge(key, amount, Integer::sum);
    }


    public int getTotal(String playerName) {
        return totalMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public int getTotalToday(String playerName) {
        return todayMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public int getTotalMonth(String playerName) {
        return monthMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public int getTotalYear(String playerName) {
        return yearMap.getOrDefault(playerName.toLowerCase(), 0);
    }

    public List<Map.Entry<String, Integer>> getTopToday(int limit) {
        return sortDesc(todayMap, limit);
    }

    public List<Map.Entry<String, Integer>> getTopMonth(int limit) {
        return sortDesc(monthMap, limit);
    }

    public List<Map.Entry<String, Integer>> getTopYear(int limit) {
        return sortDesc(yearMap, limit);
    }

    public List<Map.Entry<String, Integer>> getTopTotal(int limit) {
        return sortDesc(totalMap, limit);
    }

    private List<Map.Entry<String, Integer>> sortDesc(Map<String, Integer> map, int limit) {
        return map.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit).collect(Collectors.toList());
    }

    private void loadDoneMilestones() {
        if (!milestoneFile.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(milestoneFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length < 2) {
                    continue;
                }
                Set<Integer> done = ConcurrentHashMap.newKeySet();
                for (String m : parts[1].split(",")) {
                    try {
                        done.add(Integer.parseInt(m.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                playerMilestones.put(parts[0].toLowerCase(), done);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không đọc được milestone_done.yml: " + e.getMessage());
        }
    }

    public Set<Integer> getDoneMilestones(String playerName) {
        return playerMilestones.getOrDefault(playerName.toLowerCase(), Collections.emptySet());
    }

    public void saveDoneMilestones(String playerName, Set<Integer> doneMilestones) {
        playerMilestones.put(playerName.toLowerCase(), doneMilestones);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(milestoneFile))) {
            for (Map.Entry<String, Set<Integer>> e : playerMilestones.entrySet()) {
                writer.write(e.getKey() + ":" + String.join(",", e.getValue().stream().map(String::valueOf).toArray(String[]::new)));
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không lưu được milestone_done.yml: " + e.getMessage());
        }
    }

    public void checkMilestones(String playerName, Player player) {
        if (!plugin.getConfig().getBoolean("milestones.enable", false)) {
            return;
        }

        int total = getTotal(playerName);
        String key = playerName.toLowerCase();


        Set<Integer> doneSet = playerMilestones.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());

        plugin.getConfig().getConfigurationSection("milestones.command").getKeys(false).stream().map(Integer::parseInt).sorted().forEach(th -> {
            if (total >= th && doneSet.add(th)) {
                plugin.getConfig().getStringList("milestones.command." + th).forEach(cmd -> execute(cmd, player));
                saveDoneMilestones(key, doneSet);
                player.sendMessage("§aChúc mừng bạn đã đạt mốc nạp §e" + th + "đ§a!");
            }
        });
    }


    private void execute(String cmd, Player player) {
        String real = cmd.replace("{player}", player.getName());
        if (real.startsWith("console:")) {
            NapThePlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real.substring(8)));
        } else if (real.startsWith("player:")) {

            NapThePlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> player.performCommand(real.substring(7)));
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
            NapThePlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real));
        }
    }


    public CardDataCache getCache() {
        return plugin.getCardDataCache();
    }
}
