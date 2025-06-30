package org.Card2K.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.Card2K.NapThePlugin;

import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final NapThePlugin plugin;
    private final FileConfiguration config;

    public ConfigManager(NapThePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public String getPartnerId() {
        return config.getString("Card2kAPI.key", "");
    }

    public String getPartnerKey() {
        return config.getString("Card2kAPI.secret", "");
    }

    public boolean isFastCommandEnabled() {
        return config.getBoolean("fastcmd", true);
    }

    public List<String> getAcceptedCardTypes() {
        return config.getStringList("card.enable");
    }

    public Map<String, Object> getAllCommands() {
        if (config.getConfigurationSection("card.command") != null) {
            return config.getConfigurationSection("card.command").getValues(false);
        }
        return Map.of();
    }

    public Map<String, Object> getMilestoneCommands() {
        if (config.getConfigurationSection("milestones.command") != null) {
            return config.getConfigurationSection("milestones.command").getValues(false);
        }
        return Map.of();
    }

    public boolean isMilestoneEnabled() {
        return config.getBoolean("enable", false);
    }

    public long getPlaceholderUpdateInterval() {
        return config.getLong("placeholder_update", 300L);
    }

    public int getDelayBeforeReward() {
        return config.getInt("delay_before_reward", 5);
    }

    public int getMaxRetry() {
        return config.getInt("max_retry", 1);
    }
}
