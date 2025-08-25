package org.Card2K.util;

import org.Card2K.NapThePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandExecutor {

    public static void executeCommands(Player player, int amount) {
        String path = "card.command." + amount;

        if (!NapThePlugin.getInstance().getConfig().contains(path)) {
            NapThePlugin.getInstance().getLogger().warning("⚠ Không tìm thấy lệnh cho mệnh giá: " + amount);
            return;
        }

        List<String> commands = NapThePlugin.getInstance().getConfig().getStringList(path);
        if (commands.isEmpty()) {
            NapThePlugin.getInstance().getLogger().warning("⚠ Danh sách lệnh rỗng cho mệnh giá: " + amount);
            return;
        }

        for (String rawCmd : commands) {
            String cmd = rawCmd.replace("{player}", player.getName());

            // DEBUG
            if (NapThePlugin.getInstance().getConfig().getBoolean("debug")) {
                NapThePlugin.getInstance().getLogger().info("[DEBUG] Thực thi: " + cmd);
            }

            try {
                if (cmd.startsWith("console:")) {
                    String real = cmd.substring("console:".length());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
                } else if (cmd.startsWith("player:")) {
                    String real = cmd.substring("player:".length());
                    player.performCommand(real);
                } else if (cmd.startsWith("me ")) {
                    player.chat("/" + cmd);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            } catch (Exception e) {
                NapThePlugin.getInstance().getLogger().severe("❌ Lỗi khi thực hiện lệnh: " + cmd);
                e.printStackTrace();
            }
        }
    }
}
