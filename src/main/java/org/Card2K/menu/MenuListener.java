package org.Card2K.menu;

import org.Card2K.NapThePlugin;
import org.Card2K.gui.AnvilInputManager;
import org.Card2K.util.CardSelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType;

public class MenuListener implements Listener {
    private final NapThePlugin plugin;

    public MenuListener(NapThePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || !isValidItem(event.getCurrentItem())) return;
        if (event.getView().getTopInventory().getType() == InventoryType.ANVIL) return;

        event.setCancelled(true);

        String clickedName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).trim();
        String menuTitle = ChatColor.stripColor(event.getView().getTitle()).trim();

        processMenuClick(player, menuTitle, clickedName);
    }

    private boolean isValidItem(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }

    private void processMenuClick(Player player, String menuTitle, String clickedName) {
        ConfigurationSection menus = plugin.getMenusConfig().getConfigurationSection("menus");
        if (menus == null) return;

        for (String menuKey : menus.getKeys(false)) {
            ConfigurationSection menu = menus.getConfigurationSection(menuKey);
            if (menu == null) continue;

            String title = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', menu.getString("menu_title", ""))).trim();
            if (!menuTitle.equalsIgnoreCase(title) && !menuTitle.contains(title) && !title.contains(menuTitle)) continue;

            ConfigurationSection items = menu.getConfigurationSection("items");
            if (items == null) continue;

            for (String itemKey : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(itemKey);
                if (item == null) continue;

                String itemName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getString("display_name", itemKey))).trim();
                if (!clickedName.equalsIgnoreCase(itemName) && !clickedName.contains(itemName) && !itemName.contains(clickedName)) continue;

                String action = item.getString("action", "").trim();
                if (!action.isEmpty()) handleAction(player, action);
                return;
            }
        }
    }

    private void handleAction(Player player, String action) {
        String[] parts = action.split(" ");
        for (int i = 0; i < parts.length; i++) {
            String token = parts[i].toLowerCase().trim();
            switch (token) {
                case "choosecard":
                    if (i + 1 < parts.length) {
                        setTelco(player, parts[i + 1].trim());
                        i++;
                    }
                    break;
                case "chooseprice":
                    if (i + 1 < parts.length) {
                        setPrice(player, parts[i + 1].trim());
                        i++;
                    }
                    break;
                case "openmenu":
                    if (i + 1 < parts.length) {
                        new MenuManager(plugin).openMenu(player, parts[i + 1].trim());
                        i++;
                    }
                    break;
                default:
                    player.sendMessage("§cAction không hỗ trợ: " + token);
                    break;
            }
        }
    }

    private void setTelco(Player player, String telco) {
        CardSelectionManager.setTelco(player.getUniqueId(), telco.toLowerCase());
        player.sendMessage("§aBạn đã chọn loại thẻ: §e" + telco);

        if (plugin.getMenusConfig().isConfigurationSection("menus.chon_menh_gia")) {
            new MenuManager(plugin).openMenu(player, "chon_menh_gia");
        } else {
            new MenuManager(plugin).openMenuByCommand(player, "menhgia");
        }
    }

    private void setPrice(Player player, String priceStr) {
        try {
            int amount = Integer.parseInt(priceStr);
            CardSelectionManager.setAmount(player.getUniqueId(), amount);

            String telco = CardSelectionManager.getTelco(player.getUniqueId());
            if (telco == null) {
                player.sendMessage("§cBạn chưa chọn loại thẻ. Vui lòng chọn lại.");
                return;
            }

            new AnvilInputManager(plugin).startInput(player, telco, amount);
        } catch (NumberFormatException e) {
            player.sendMessage("§cMệnh giá không hợp lệ: " + priceStr);
        }
    }
}
