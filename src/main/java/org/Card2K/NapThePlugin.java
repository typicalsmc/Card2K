package org.Card2K;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.Card2K.command.NapTheCommand;
import org.Card2K.manager.ConfigManager;
import org.Card2K.manager.DataManager;
import org.Card2K.util.CardDataCache;
import org.Card2K.util.CardDataManager;
import org.Card2K.placeholder.CardPlaceholder;

public class NapThePlugin extends JavaPlugin {

    private static NapThePlugin instance;
    private ConfigManager configManager;
    private DataManager dataManager;
    private CardDataManager cardDataManager;
    private CardDataCache cardDataCache;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("Đang khởi tạo plugin NapThePlugin...");

        configManager = new ConfigManager(this);
        cardDataCache = new CardDataCache(this);
        dataManager = new DataManager(this);
        cardDataManager = new CardDataManager(this);

        cardDataCache.reload();

        getCommand("napthe").setExecutor(new NapTheCommand(this));

        long placeholderTick = getConfig().getLong("placeholder_update", 300L);
        getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                () -> cardDataCache.reload(),
                placeholderTick,
                placeholderTick
        );

        // Đăng ký PlaceholderAPI nếu có
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CardPlaceholder(this, cardDataCache).register();
            getLogger().info("Đã hook vào PlaceholderAPI.");
        } else {
            getLogger().warning("Không tìm thấy PlaceholderAPI. Một số chức năng sẽ bị vô hiệu.");
        }

        getLogger().info("NapThePlugin đã bật thành công!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NapThePlugin đã tắt.");
    }

    public static NapThePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CardDataManager getCardDataManager() {
        return cardDataManager;
    }

    public CardDataCache getCardDataCache() {
        return cardDataCache;
    }

    public void reloadPlugin() {
        reloadConfig();
        getLogger().info("Reload plugin...");

        configManager = new ConfigManager(this);
        cardDataCache.reload();
        dataManager.reload();
        cardDataManager = new CardDataManager(this);

        getServer().getScheduler().cancelTasks(this);
        long placeholderTick = getConfig().getLong("placeholder_update", 300L);
        getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                () -> cardDataCache.reload(),
                placeholderTick,
                placeholderTick
        );

        getLogger().info("NapThePlugin đã được reload thành công!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("napthe") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("card2k.reload")) {
                sender.sendMessage("§cBạn không có quyền thực hiện lệnh này.");
                return true;
            }
            reloadPlugin();
            sender.sendMessage("§a✔ Đã reload plugin thành công.");
            return true;
        }
        return false;
    }
}
