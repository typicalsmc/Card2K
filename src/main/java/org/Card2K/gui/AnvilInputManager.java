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
        // Item hướng dẫn nhập mã thẻ
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta paperMeta = paper.getItemMeta();
        paperMeta.setDisplayName("§e✏ Nhập mã thẻ ở đây");
        paperMeta.setLore(Arrays.asList("§7VD: 12345678901234", "§8> Nhấn enter để tiếp tục"));
        paper.setItemMeta(paperMeta);

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("🔐 Nhập mã thẻ cào")
                .text("Nhập mã thẻ...")
                .itemLeft(paper)
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String code = stateSnapshot.getText().trim().replace(" ", "");
                    if (code.length() < 6 || code.equalsIgnoreCase("Nhập mã thẻ...")) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return Collections.singletonList(
                                AnvilGUI.ResponseAction.replaceInputText("")
                        );
                    }

                    ItemStack serialItem = new ItemStack(Material.NAME_TAG);
                    ItemMeta serialMeta = serialItem.getItemMeta();
                    serialMeta.setDisplayName("§e✏ Nhập số serial");
                    serialMeta.setLore(Arrays.asList("§7VD: 1234567890", "§8> Nhấn enter để gửi"));
                    serialItem.setItemMeta(serialMeta);

                    new AnvilGUI.Builder()
                            .plugin(plugin)
                            .title("🔢 Nhập serial thẻ")
                            .text("Nhập serial...")
                            .itemLeft(serialItem)
                            .onClick((slot2, state2) -> {
                                if (slot2 != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                                String serial = state2.getText().trim().replace(" ", "");
                                if (serial.length() < 6 || serial.equalsIgnoreCase("Nhập serial...")) {
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                                    return Collections.singletonList(
                                            AnvilGUI.ResponseAction.replaceInputText("")
                                    );
                                }

                                String requestId = UUID.randomUUID().toString().replace("-", "");
                                CardRequest.requestCard(plugin, player, cardType, code, serial, amount, requestId);

                                CardSelectionManager.clear(player.getUniqueId());
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                                return Collections.singletonList(AnvilGUI.ResponseAction.close());
                            })
                            .open(player);

                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    public void handleFastCommand(Player player, String telco, int amount, String serial, String code) {
        if (code.length() < 6 || serial.length() < 6) {
            player.sendMessage("§cMã thẻ hoặc serial không hợp lệ!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        CardRequest.requestCard(plugin, player, telco, code, serial, amount, requestId);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }
}
