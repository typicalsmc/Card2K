package org.Card2K.menu;

import me.clip.placeholderapi.PlaceholderAPI;
import org.Card2K.NapThePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class MenuManager {
    private final NapThePlugin plugin;

    public MenuManager(NapThePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenuByCommand(Player player, String command) {
        if (command == null || command.trim().isEmpty()) {
            player.sendMessage("§cCommand không hợp lệ.");
            return;
        }

        String cmdLower = command.toLowerCase().trim();
        ConfigurationSection menus = plugin.getMenusConfig().getConfigurationSection("menus");
        if (menus == null) {
            player.sendMessage("§cKhông tìm thấy menus trong cấu hình.");
            return;
        }

        for (String menuKey : menus.getKeys(false)) {
            ConfigurationSection menu = menus.getConfigurationSection(menuKey);
            if (menu == null) continue;

            Object openObj = menu.get("open_command");
            if (openObj == null) continue;

            if (openObj instanceof String && ((String) openObj).equalsIgnoreCase(cmdLower)) {
                openMenu(player, menuKey);
                return;
            }

            if (openObj instanceof List<?>) {
                for (String c : menu.getStringList("open_command")) {
                    if (c != null && c.equalsIgnoreCase(cmdLower)) {
                        openMenu(player, menuKey);
                        return;
                    }
                }
            }
        }

        player.sendMessage("§cKhông tìm thấy menu để mở.");
    }

    public void openMenu(Player player, String menuName) {
        if (menuName == null || menuName.trim().isEmpty()) {
            player.sendMessage("§cMenu không hợp lệ.");
            return;
        }

        ConfigurationSection menuSec = plugin.getMenusConfig().getConfigurationSection("menus." + menuName);
        if (menuSec == null) {
            player.sendMessage("§cMenu không tồn tại: " + menuName);
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&',
                PlaceholderAPI.setPlaceholders(player, menuSec.getString("menu_title", "&aMenu")));
        int size = Math.min(Math.max(menuSec.getInt("size", 27), 9), 54);
        size = (size + 8) / 9 * 9;

        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection items = menuSec.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSec = items.getConfigurationSection(key);
                if (itemSec == null) continue;

                int slot = itemSec.getInt("slot", -1);
                Material mat = Material.matchMaterial(itemSec.getString("material", "PAPER"));
                if (mat == null) mat = Material.PAPER;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {

                    String display = itemSec.getString("display_name", key);
                    display = PlaceholderAPI.setPlaceholders(player, display);
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', display));

                    List<String> lore = itemSec.getStringList("lore").stream()
                            .map(s -> PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', s)))
                            .collect(Collectors.toList());
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                }

                if (slot >= 0 && slot < size) {
                    inv.setItem(slot, item);
                }
            }
        }

        player.openInventory(inv);
    }
}
