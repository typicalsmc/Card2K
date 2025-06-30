package org.Card2K.command;

import org.Card2K.NapThePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final NapThePlugin plugin;

    public ReloadCommand(NapThePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("card2k.reload")) {
            sender.sendMessage("§cBạn không có quyền thực hiện lệnh này.");
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage("§aPlugin đã được reload thành công!");
        return true;
    }
}
