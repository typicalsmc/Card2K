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
        return "1.0.1";
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

        // Tổng cộng
        switch (param) {
            case "total":
                return String.valueOf(cache.getTotal(playerName));
            case "total_month":
                return String.valueOf(cache.getTotalMonth(playerName));
            case "total_year":
                return String.valueOf(cache.getTotalYear(playerName));
        }

        if (param.startsWith("top_") && param.endsWith("_amount")) {
            String[] split = param.split("_");
            if (split.length == 4 && split[3].equals("amount")) {
                String type = split[1]; // month, year, total
                int rank;
                try {
                    rank = Integer.parseInt(split[2]);
                } catch (NumberFormatException e) {
                    return "N/A";
                }
                if (rank < 1 || rank > 10) return "N/A";

                List<Map.Entry<String, Integer>> topList;
                switch (type) {
                    case "month":
                        topList = cache.getTopMonth(10);
                        break;
                    case "year":
                        topList = cache.getTopYear(10);
                        break;
                    case "total":
                        topList = cache.getTopTotal(10);
                        break;
                    default:
                        return "N/A";
                }

                if (rank <= topList.size()) {
                    return String.valueOf(topList.get(rank - 1).getValue());
                } else {
                    return "N/A";
                }
            }
        }

        if (param.startsWith("top_")) {
            String[] split = param.split("_");
            if (split.length == 3) {
                String type = split[1]; // month, year, total
                int rank;
                try {
                    rank = Integer.parseInt(split[2]);
                } catch (NumberFormatException e) {
                    return "N/A";
                }
                if (rank < 1 || rank > 10) return "N/A";

                List<Map.Entry<String, Integer>> topList;
                switch (type) {
                    case "month":
                        topList = cache.getTopMonth(10);
                        break;
                    case "year":
                        topList = cache.getTopYear(10);
                        break;
                    case "total":
                        topList = cache.getTopTotal(10);
                        break;
                    default:
                        topList = Collections.emptyList();
                }

                if (rank <= topList.size()) {
                    return topList.get(rank - 1).getKey();
                } else {
                    return "N/A";
                }
            }
        }

        return "";
    }
}
