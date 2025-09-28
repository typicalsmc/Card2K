package org.Card2K.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.Card2K.NapThePlugin;
import org.Card2K.util.CardDataCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CardPlaceholder extends PlaceholderExpansion {

    private final NapThePlugin plugin;
    private final CardDataCache cache;

    public CardPlaceholder(NapThePlugin plugin, CardDataCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }

    @Override
    public String getIdentifier() {
        return "card2k";
    }

    @Override
    public String getAuthor() {
        return "Card2K";
    }

    @Override
    public String getVersion() {
        return "1.0.4";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return "";

        String playerName = (player != null && player.getName() != null)
                ? player.getName().toLowerCase()
                : "";
        String param = params.toLowerCase();

        switch (param) {
            case "total":
                return format(cache.getTotal(playerName));
            case "total_month":
                return format(cache.getTotalMonth(playerName));
            case "total_year":
                return format(cache.getTotalYear(playerName));
        }

        if (param.startsWith("top_") && param.endsWith("_amount")) {
            String[] split = param.split("_");
            if (split.length == 4 && split[3].equals("amount")) {
                String type = split[1];
                int rank;
                try {
                    rank = Integer.parseInt(split[2]);
                } catch (NumberFormatException e) {
                    return "N/A";
                }
                if (rank < 1 || rank > 10) return "N/A";

                List<Map.Entry<String, Integer>> topList = getTopList(type);
                if (rank <= topList.size()) {
                    return format(topList.get(rank - 1).getValue());
                }
                return "N/A";
            }
        }


        if (param.startsWith("top_")) {
            String[] split = param.split("_");
            if (split.length == 3) {
                String type = split[1];
                int rank;
                try {
                    rank = Integer.parseInt(split[2]);
                } catch (NumberFormatException e) {
                    return "N/A";
                }
                if (rank < 1 || rank > 10) return "N/A";

                List<Map.Entry<String, Integer>> topList = getTopList(type);
                if (rank <= topList.size()) {
                    Map.Entry<String, Integer> entry = topList.get(rank - 1);
                    return formatTopDisplay(rank, entry.getKey(), entry.getValue());
                }
                return "N/A";
            }
        }

        return "";
    }

    private List<Map.Entry<String, Integer>> getTopList(String type) {
        switch (type) {
            case "month":
                return cache.getTopMonth(10);
            case "year":
                return cache.getTopYear(10);
            case "total":
                return cache.getTopTotal(10);
            default:
                return Collections.emptyList();
        }
    }

    private String formatTopDisplay(int rank, String name, int amount) {
        String color;
        switch (rank) {
            case 1: color = "§6"; break;
            case 2: color = "§b"; break;
            case 3: color = "§a"; break;
            default: color = "§e"; break;
        }
        return color + name + " §7- §f" + format(amount) + "đ";
    }

    private String format(int value) {
        return String.format("%,d", value);
    }
}
