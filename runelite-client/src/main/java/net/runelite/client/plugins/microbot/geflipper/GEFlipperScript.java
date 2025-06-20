package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.api.GameState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import org.apache.commons.lang3.tuple.Pair;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.item.Rs2ItemManager;

import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {
    public static String status = "Idle";
    public static int profit = 0;
    private static long startTime;
    private final Rs2ItemManager itemManager = new Rs2ItemManager();

    private final List<String> f2pItems = new ArrayList<>();

    private static final long TRADE_LIMIT_MS = TimeUnit.HOURS.toMillis(4);
    private final Map<String, Long> lastFlipped = new HashMap<>();

    private List<String> loadF2pItems()
    {
        return Microbot.getClientThread().runOnClientThread(() ->
        {
            Set<String> set = new HashSet<>();
            for (Field f : ItemID.class.getFields())
            {
                if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int.class)
                    continue;
                try
                {
                    int id = f.getInt(null);
                    ItemComposition comp = Microbot.getItemManager().getItemComposition(id);
                    if (comp != null && !comp.isMembers() && comp.isTradeable())
                    {
                        set.add(comp.getName());
                    }
                }
                catch (Exception ignored)
                {
                }
            }
            return new ArrayList<>(set);
        });
    }

    public boolean run(GEFlipperConfig config) {
        Rs2AntibanSettings.naturalMouse = true;
        Rs2GrandExchange.setGeTrackerKey(config.apiKey());
        startTime = System.currentTimeMillis();
        if (f2pItems.isEmpty())
        {
            f2pItems.addAll(loadF2pItems());
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!Microbot.isLoggedIn() || Microbot.getClient().getGameState() != GameState.LOGGED_IN)
                return;
            if (!super.run())
                return;
            try {
                if (!Rs2GrandExchange.isOpen()) {
                    status = "Opening GE";
                    Rs2GrandExchange.openExchange();
                    return;
                }
                int gp = Rs2Inventory.count("Coins");
                for (String itemName : f2pItems) {
                    Pair<GrandExchangeSlots, Integer> availableSlot = Rs2GrandExchange.getAvailableSlot();
                    if (availableSlot.getLeft() == null || availableSlot.getLeft().ordinal() >= 3) {
                        status = "Waiting for slot";
                        break;
                    }
                    long last = lastFlipped.getOrDefault(itemName, 0L);
                    if (System.currentTimeMillis() - last < TRADE_LIMIT_MS) {
                        status = "Trade limit";
                        continue;
                    }

                    int itemId = itemManager.getItemId(itemName);
                    int highPrice = Rs2GrandExchange.getOfferPrice(itemId); // GE buying price
                    int lowPrice = Rs2GrandExchange.getSellPrice(itemId);  // GE selling price
                    int sellingVolume = Rs2GrandExchange.getSellingVolume(itemId);
                    int currentBuyVolume = Rs2GrandExchange.getBuyingVolume(itemId);
                    if (highPrice <= 0 || lowPrice <= 0 || sellingVolume < 0 || currentBuyVolume < 0) {
                        status = "API error";
                        continue;
                    }
                    int itemMargin = highPrice - lowPrice;
                    if (itemMargin < config.minMargin() || sellingVolume < config.minVolume() || currentBuyVolume < config.minVolume()) {
                        status = "Low vol/margin";
                        continue;
                    }
                    int quantity = Math.min(gp / lowPrice, 100); // simple calc
                    if (quantity <= 0)
                        continue;
                    status = "Buying " + itemName;
                    if (Rs2GrandExchange.buyItem(itemName, lowPrice, quantity)) {
                        Rs2GrandExchange.collectToInventory();
                        Rs2GrandExchange.sellItem(itemName, quantity, highPrice);
                        profit += itemMargin * quantity;
                        lastFlipped.put(itemName, System.currentTimeMillis());
                        break;
                    }
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);
        return true;
    }

    public static String getProfitPerHour() {
        long timeRan = System.currentTimeMillis() - startTime;
        if (timeRan <= 0) {
            return "0";
        }
        double ph = profit * 3600000d / timeRan;
        return String.format("%,.0f", ph);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2AntibanSettings.naturalMouse = false;
        startTime = 0;
    }
}