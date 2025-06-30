package org.Card2K.util;

import java.util.HashMap;
import java.util.UUID;

public class CardSelectionManager {
    private static final HashMap<UUID, String> telcoMap = new HashMap<>();
    private static final HashMap<UUID, Integer> amountMap = new HashMap<>();

    public static void setTelco(UUID uuid, String telco) {
        telcoMap.put(uuid, telco);
    }

    public static void setAmount(UUID uuid, int amount) {
        amountMap.put(uuid, amount);
    }

    public static String getTelco(UUID uuid) {
        return telcoMap.get(uuid);
    }

    public static Integer getAmount(UUID uuid) {
        return amountMap.get(uuid);
    }

    public static boolean isReady(UUID uuid) {
        return telcoMap.containsKey(uuid) && amountMap.containsKey(uuid);
    }

    public static void clear(UUID uuid) {
        telcoMap.remove(uuid);
        amountMap.remove(uuid);
    }
}
