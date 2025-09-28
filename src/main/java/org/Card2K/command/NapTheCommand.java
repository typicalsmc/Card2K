package org.Card2K.command;

import org.Card2K.NapThePlugin;
import org.Card2K.gui.AnvilInputManager;
import org.Card2K.menu.MenuManager;
import org.Card2K.util.CardSelectionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NapTheCommand implements CommandExecutor {

    private final NapThePlugin plugin;

    public NapTheCommand(NapThePlugin plugin) {
        this.plugin = plugin;
        if (plugin.getCommand("napthe") != null) {
            plugin.getCommand("napthe").setExecutor(this);
        } else {
            plugin.getLogger().warning("Command 'napthe' not defined in plugin.yml");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới có thể sử dụng lệnh này.");
            return true;
        }

        if (args.length == 0) {
            new MenuManager(plugin).openMenu(player, "chon_loai_the");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            new MenuManager(plugin).openMenu(player, "top_nap_the");
            return true;
        }

        if (args.length == 2) {
            return handleOpenInput(player, args);
        }

        if (args.length >= 4) {
            return handleFastCommand(player, args);
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
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

    private boolean handleOpenInput(Player player, String[] args) {
        String telco = args[0].toLowerCase();
        try {
            int amount = Integer.parseInt(args[1]);
            CardSelectionManager.setTelco(player.getUniqueId(), telco);
            CardSelectionManager.setAmount(player.getUniqueId(), amount);
            new AnvilInputManager(plugin).startInput(player, telco, amount);
        } catch (NumberFormatException ex) {
            player.sendMessage("§cMệnh giá không hợp lệ. Ví dụ: /napthe viettel 10000");
        }
        return true;
    }

    private boolean handleFastCommand(Player player, String[] args) {
        String telco = args[0].toLowerCase();
        try {
            int amount = Integer.parseInt(args[1]);
            String serial = args[2];
            String code = args[3];
            new AnvilInputManager(plugin).handleFastCommand(player, telco, amount, serial, code);
        } catch (NumberFormatException ex) {
            player.sendMessage("§cMệnh giá không hợp lệ. Ví dụ: /napthe viettel 10000 123456 987654321");
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eSử dụng: /napthe để mở menu");
        sender.sendMessage("§eHoặc: /napthe <telco> <amount> (mở nhập mã & serial)");
        sender.sendMessage("§eHoặc: /napthe <telco> <amount> <serial> <code> (gửi nhanh)");
        sender.sendMessage("§eHoặc: /napthe top  — xem top nạp");
        sender.sendMessage("§eHoặc: /napthe reload  — reload plugin ");
    }
}
