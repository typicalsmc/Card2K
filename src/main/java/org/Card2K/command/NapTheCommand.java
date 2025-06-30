package org.Card2K.command;

import org.Card2K.NapThePlugin;
import org.Card2K.gui.AnvilInputManager;
import org.Card2K.util.CardSelectionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NapTheCommand implements CommandExecutor {

    private final NapThePlugin plugin;

    public NapTheCommand(NapThePlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("napthe").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cLệnh này chỉ sử dụng trong game.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (plugin.getConfigManager().isFastCommandEnabled()) {
                String telco = CardSelectionManager.getTelco(player.getUniqueId());
                int amount = CardSelectionManager.getAmount(player.getUniqueId());
                if (telco == null || amount <= 0) {
                    player.sendMessage("§cBạn chưa chọn loại thẻ hoặc mệnh giá. Dùng §e/napthe help §cđể xem hướng dẫn.");
                    return true;
                }

                AnvilInputManager input = new AnvilInputManager(plugin);
                input.startInput(player, telco, amount);
                return true;
            } else {
                sendHelp(player);
                return true;
            }
        }

        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        if (subCommand.equalsIgnoreCase("choosecard")) {
            if (args.length < 2) {
                player.sendMessage("§cVui lòng nhập loại thẻ. VD: /napthe choosecard viettel");
                return true;
            }

            String telco = args[1].toLowerCase();
            CardSelectionManager.setTelco(player.getUniqueId(), telco);
            player.sendMessage("§aĐã chọn loại thẻ: §e" + telco);
            return true;
        }

        if (subCommand.equalsIgnoreCase("choosecardprice")) {
            if (args.length < 2) {
                player.sendMessage("§cVui lòng nhập mệnh giá. VD: /napthe choosecardprice 10000");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cMệnh giá không hợp lệ.");
                return true;
            }

            CardSelectionManager.setAmount(player.getUniqueId(), amount);

            String telco = CardSelectionManager.getTelco(player.getUniqueId());
            if (telco == null) {
                player.sendMessage("§cBạn chưa chọn loại thẻ. Dùng /napthe choosecard <loại>");
                return true;
            }

            AnvilInputManager input = new AnvilInputManager(plugin);
            input.startInput(player, telco, amount);
            return true;
        }

        if (plugin.getConfigManager().isFastCommandEnabled() && args.length == 4) {
            String telco = args[0];
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cMệnh giá không hợp lệ.");
                return true;
            }
            String serial = args[2];
            String code = args[3];

            AnvilInputManager input = new AnvilInputManager(plugin);
            input.handleFastCommand(player, telco, amount, serial, code);
            return true;
        }

        player.sendMessage("§cLệnh không hợp lệ. Dùng §e/napthe help §cđể xem hướng dẫn.");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6[Card2k] §fHướng dẫn sử dụng:");
        player.sendMessage("§e/napthe để mở gui nạp thẻ");
        player.sendMessage("§e/napthe choosecard <loại> §7- Chọn loại thẻ (viettel, vina, mobi...)");
        player.sendMessage("§e/napthe choosecardprice <giá trị> §7- Nhập mệnh giá và mở giao diện nhập mã");
        player.sendMessage("§e/napthe <loại thẻ> <giá trị> <serial> <mã thẻ> §7- Nạp nhanh nếu bật fastcmd");
    }
}
