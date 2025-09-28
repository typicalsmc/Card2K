package org.Card2K.command;

import org.Card2K.NapThePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final NapThePlugin plugin;

    public ReloadCommand(NapThePlugin plugin) {
        this.plugin = plugin;
        if (plugin.getCommand("card2kreload") != null) {
            plugin.getCommand("card2kreload").setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("card2k.reload")) {
            sender.sendMessage("§cBạn không có quyền thực hiện lệnh này.");
            return true;
        }

        sender.sendMessage("§eĐang reload plugin Card2K...");
        try {
            plugin.reloadPlugin();
            sender.sendMessage("§a✔ Plugin Card2K đã reload thành công.");
        } catch (Exception e) {
            sender.sendMessage("§c✘ Reload plugin thất bại: " + e.getMessage());
            plugin.getLogger().severe("Lỗi khi reload plugin: " + e);
        }
        return true;
    }
}
