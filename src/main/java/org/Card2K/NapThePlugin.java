package org.Card2K;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import org.Card2K.command.NapTheCommand;
import org.Card2K.manager.ConfigManager;
import org.Card2K.manager.DataManager;
import org.Card2K.placeholder.CardPlaceholder;
import org.Card2K.scheduler.PaperSchedulerAdapter;
import org.Card2K.scheduler.SpigotSchedulerAdapter;
import org.Card2K.scheduler.SchedulerAdapter;
import org.Card2K.menu.MenuListener;
import org.Card2K.util.CardDataCache;
import org.Card2K.util.CardDataManager;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class NapThePlugin extends JavaPlugin {
    private static NapThePlugin instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CardDataManager cardDataManager;
    private CardDataCache cardDataCache;
    private SchedulerAdapter scheduler;
    private FileConfiguration menusConfig;
    private String detectedServerType = "Unknown";

    private final ConcurrentLinkedQueue<Runnable> mainQueue = new ConcurrentLinkedQueue<>();

    private final ScheduledExecutorService httpExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Card2K-HTTP-Executor");
            t.setDaemon(true);
            return t;
        }
    });

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("Đang khởi tạo plugin NapThePlugin...");

        configManager = new ConfigManager(this);
        cardDataCache = new CardDataCache(this);
        dataManager = new DataManager(this);
        cardDataManager = new CardDataManager(this);

        loadMenusConfig();

        try {
            cardDataCache.reload();
        } catch (Exception ex) {
            getLogger().warning("Lỗi khi reload cardDataCache ban đầu: " + ex.getMessage());
        }

        new NapTheCommand(this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        scheduler = detectScheduler();
        if (scheduler != null) {
            getLogger().info("⚙ Scheduler type: " + detectedServerType);
            scheduler.startRepeatingTask(this::runMainQueue, 50L);

            long placeholderTick = getConfig().getLong("placeholder_update", 300L);
            scheduler.startRepeatingTask(() -> {
                try {
                    cardDataCache.reload();
                } catch (Exception ex) {
                    getLogger().warning("Error reloading card cache: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }, placeholderTick);
        } else {
            Bukkit.getScheduler().runTaskTimer(this, this::runMainQueue, 50L, 50L);
            getLogger().warning("⚠ SchedulerAdapter not available; falling back to Bukkit scheduler for mainQueue.");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CardPlaceholder(this, cardDataCache).register();
            getLogger().info("§aHook vào PlaceholderAPI thành công.");
        }

        getLogger().info("NapThePlugin đã bật thành công!");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            try {
                scheduler.cancelAll();
            } catch (Exception ex) {
                getLogger().warning("Lỗi khi hủy tất cả task: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        try {
            httpExecutor.shutdownNow();
        } catch (Exception ex) {
            getLogger().warning("Lỗi khi shutdown httpExecutor: " + ex.getMessage());
        }

        getLogger().info("NapThePlugin đã tắt.");
    }

    public static NapThePlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public CardDataManager getCardDataManager() { return cardDataManager; }
    public CardDataCache getCardDataCache() { return cardDataCache; }
    public SchedulerAdapter getSchedulerAdapter() { return scheduler; }

    public void enqueueMain(Runnable runnable) {
        if (runnable != null) mainQueue.add(runnable);
        else getLogger().warning("⚠ Đang cố enqueue một Runnable null!");
    }

    public static void runOnMainThread(Runnable runnable) {
        if (instance == null) return;
        try {
            SchedulerAdapter s = instance.getSchedulerAdapter();
            if (s != null) {
                s.runTask(instance, runnable);
                return;
            }
        } catch (Throwable ignored) {}
        Bukkit.getScheduler().runTask(instance, runnable);
    }

    public FileConfiguration getMenusConfig() {
        if (menusConfig == null) loadMenusConfig();
        return menusConfig;
    }

    private void loadMenusConfig() {
        try {
            File menusFile = new File(getDataFolder(), "menus.yml");
            if (!menusFile.exists()) {
                saveResource("menus.yml", false);
                getLogger().info("Đã tạo menus.yml mặc định.");
            }
            menusConfig = YamlConfiguration.loadConfiguration(menusFile);
        } catch (Exception ex) {
            getLogger().severe("Lỗi khi nạp menus.yml: " + ex.getMessage());
        }
    }

    public String getDetectedServerType() { return detectedServerType; }

    public void reloadPlugin() {
        getLogger().info("🔄 Bắt đầu reload Card2K...");
        try {
            reloadConfig();
            loadMenusConfig();

            configManager = new ConfigManager(this);
            try { cardDataCache.reload(); }
            catch (Exception ex) { getLogger().warning("Lỗi reload cardDataCache: " + ex.getMessage()); }

            dataManager.reload();
            cardDataManager = new CardDataManager(this);

            if (scheduler != null) {
                scheduler.cancelAll();
                long placeholderTick = getConfig().getLong("placeholder_update", 300L);
                scheduler.startRepeatingTask(() -> {
                    try { cardDataCache.reload(); }
                    catch (Exception ex) { getLogger().warning("Lỗi khi reload card cache: " + ex.getMessage()); }
                }, placeholderTick);
            }

            getLogger().info("✅ Reload Card2K thành công!");
        } catch (Exception e) {
            getLogger().severe("❌ Lỗi khi reload plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private SchedulerAdapter detectScheduler() {
        getLogger().info("⚙ Bắt đầu nhận diện loại server (Paper/Spigot)...");
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            detectedServerType = "Paper";
            getLogger().info("✔ Paper detected");
            return new PaperSchedulerAdapter(this);
        } catch (Throwable ignored) {}
        detectedServerType = "Spigot";
        getLogger().info("✔ Spigot detected");
        return new SpigotSchedulerAdapter(this);
    }

    private void runMainQueue() {
        Runnable r;
        while ((r = mainQueue.poll()) != null) {
            if (r != null) {
                try { r.run(); }
                catch (Exception ex) {
                    getLogger().warning("❌ Lỗi khi chạy task từ mainQueue: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                getLogger().warning("⚠ mainQueue chứa Runnable null!");
            }
        }
    }
}
