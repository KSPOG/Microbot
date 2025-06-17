package net.runelite.client.plugins.microbot.geflipper;

import lombok.extern.slf4j.Slf4j;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Limits {
    // Buy limits are read from the Grand Exchange interface
    private static final long FOUR_HOURS_MS = TimeUnit.HOURS.toMillis(4);

    private final Map<Integer, Integer> remainingLimits = new HashMap<>();
    private final Map<Integer, Long> resetTimes = new HashMap<>();

    private final Map<Integer, Integer> limitCache = new HashMap<>();

    public Integer fetchLimit(int itemId, String itemName) {
        Integer cached = limitCache.get(itemId);
        if (cached != null) {
            return cached;
        }

        Integer limit = Rs2GrandExchange.lookupBuyLimit(itemName, itemId);
        if (limit != null) {
            limitCache.put(itemId, limit);
        }
        return limit;
    }

    public int getRemaining(int itemId, int limit) {
        long now = System.currentTimeMillis();
        long reset = resetTimes.getOrDefault(itemId, 0L);
        if (now >= reset) {
            remainingLimits.put(itemId, limit);
            resetTimes.put(itemId, now + FOUR_HOURS_MS);
        }
        return remainingLimits.getOrDefault(itemId, limit);
    }

    public void reduceRemaining(int itemId, int qty) {
        remainingLimits.compute(itemId, (k, v) -> {
            if (v == null) return 0;
            int newVal = v - qty;
            return Math.max(newVal, 0);
        });
    }

    public void clear() {
        remainingLimits.clear();
        resetTimes.clear();
        limitCache.clear();
    }

}