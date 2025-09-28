package org.Card2K.gui;

import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.Card2K.NapThePlugin;
import org.Card2K.service.CardRequest;
import org.Card2K.util.CardSelectionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class AnvilInputManager {

    private final NapThePlugin plugin;

    public AnvilInputManager(NapThePlugin plugin) {
        this.plugin = plugin;
    }

    public void startInput(Player player, String cardType, int amount) {
        openCodeInput(player, cardType, amount);
    }

    private void openCodeInput(Player player, String cardType, int amount) {
        ItemStack paper = createItem(Material.PAPER, "§e✏ Nhập mã thẻ ở đây",
                Arrays.asList("§7VD: 12345678901234", "§8> Nhấn enter để tiếp tục"));

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("🔐 Nhập mã thẻ cào")
                .text("Nhập mã thẻ...")
                .itemLeft(paper)
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String code = sanitizeInput(stateSnapshot.getText());
                    if (code.length() < 6) {
                        playErrorSound(player);
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(""));
                    }

                    openSerialInput(player, cardType, amount, code);
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    private void openSerialInput(Player player, String cardType, int amount, String code) {
        ItemStack serialItem = createItem(Material.NAME_TAG, "§e✏ Nhập số serial",
                Arrays.asList("§7VD: 1234567890", "§8> Nhấn enter để gửi"));

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("🔢 Nhập serial thẻ")
                .text("Nhập serial...")
                .itemLeft(serialItem)
                .onClick((slot2, state2) -> {
                    if (slot2 != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String serial = sanitizeInput(state2.getText());
                    if (serial.length() < 6) {
                        playErrorSound(player);
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(""));
                    }

                    sendCardRequest(player, cardType, code, serial, amount);
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    private void sendCardRequest(Player player, String cardType, String code, String serial, int amount) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        CardRequest.requestCard(plugin, player, cardType, code, serial, amount, requestId);
        playSuccessSound(player);
        CardSelectionManager.clear(player.getUniqueId());
    }

    public void handleFastCommand(Player player, String telco, int amount, String serial, String code) {
        code = sanitizeInput(code);
        serial = sanitizeInput(serial);

        if (code.length() < 6 || serial.length() < 6) {
            player.sendMessage("§cMã thẻ hoặc serial không hợp lệ!");
            playErrorSound(player);
            return;
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        CardRequest.requestCard(plugin, player, telco, code, serial, amount, requestId);
        playSuccessSound(player);
    }


    private ItemStack createItem(Material material, String displayName, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String sanitizeInput(String input) {
        return input == null ? "" : input.trim().replace(" ", "");
    }

    private void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
    }

    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }
}
