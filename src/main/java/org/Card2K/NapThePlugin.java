package org.Card2K;

import org.Card2K.command.ReloadCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import org.Card2K.command.NapTheCommand;
import org.Card2K.manager.ConfigManager;
import org.Card2K.manager.DataManager;
import org.Card2K.placeholder.CardPlaceholder;
import org.Card2K.menu.MenuListener;
import org.Card2K.util.CardDataCache;
import org.Card2K.util.CardDataManager;
import com.tcoded.folialib.FoliaLib;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class NapThePlugin extends JavaPlugin {
    private static NapThePlugin instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CardDataManager cardDataManager;
    private CardDataCache cardDataCache;
    private FoliaLib foliaLib;
    private FileConfiguration menusConfig;


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
        new ReloadCommand(this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        foliaLib = new FoliaLib(this);
        getLogger().info("⚙ Scheduler type: " + (foliaLib.isFolia() ? "Folia" : foliaLib.isPaper() ? "Paper" : "Spigot"));


        long placeholderTick = getConfig().getLong("placeholder_update", 300L);
        foliaLib.getScheduler().runTimer(() -> {
            try {
                cardDataCache.reload();
            } catch (Exception ex) {
                getLogger().warning("Error reloading card cache: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, placeholderTick, placeholderTick);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CardPlaceholder(this, cardDataCache).register();
            getLogger().info("§aHook vào PlaceholderAPI thành công.");
        }

        getLogger().info("NapThePlugin đã bật thành công!");
    }

    @Override
    public void onDisable() {
        if (foliaLib != null) {
            try {
                foliaLib.getScheduler().cancelAllTasks();
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
    public FoliaLib getFoliaLib() { return foliaLib; }

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

    public String getDetectedServerType() {
        return foliaLib.isFolia() ? "Folia" : foliaLib.isPaper() ? "Paper" : "Spigot";
    }

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

            if (foliaLib != null) {
                foliaLib.getScheduler().cancelAllTasks();
                long placeholderTick = getConfig().getLong("placeholder_update", 300L);
                foliaLib.getScheduler().runTimer(() -> {
                    try { cardDataCache.reload(); }
                    catch (Exception ex) { getLogger().warning("Lỗi khi reload card cache: " + ex.getMessage()); }
                }, placeholderTick, placeholderTick);
            }

            getLogger().info("✅ Reload Card2K thành công!");
        } catch (Exception e) {
            getLogger().severe("❌ Lỗi khi reload plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
