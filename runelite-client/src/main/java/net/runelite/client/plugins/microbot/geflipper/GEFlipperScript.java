package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {
    private GEFlipperPlugin plugin;
    private GEFlipperConfig config;
    private final Random random = new Random();

    private final Map<Integer, Integer> bought = new HashMap<>();
    private final Map<GrandExchangeSlots, Instant> offerTimes = new HashMap<>();
    private final List<ItemComposition> f2pItems = new ArrayList<>();
    private long profit = 0;

    long getProfit() { return profit; }
    long getProfitPerHour() {
        Duration runtime = getRunTime();
        double hours = runtime.toMillis() / 3600000.0;
        if (hours <= 0) return 0;
        return (long) (profit / hours);
    }

    public boolean run(GEFlipperPlugin plugin, GEFlipperConfig config) {
        this.plugin = plugin;
        this.config = config;
        Rs2AntibanSettings.naturalMouse = true;
        loadF2P();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;

                if (BreakHandlerScript.breakIn > 0 && BreakHandlerScript.breakIn <= 180 && !BreakHandlerScript.isBreakActive()) {
                    Microbot.status = "Waiting for break";
                    Rs2GrandExchange.closeExchange();
                    return;
                }

                if (!Rs2GrandExchange.isOpen()) {
                    Rs2GrandExchange.openExchange();
                    return;
                }

                cancelOldOffers();

                if (Rs2GrandExchange.hasBoughtOffer()) {
                    Rs2GrandExchange.collectToInventory();
                }
                if (Rs2GrandExchange.hasSoldOffer()) {
                    Rs2GrandExchange.collectToInventory();
                }

                if (Rs2GrandExchange.getAvailableSlot().getLeft() == null) {
                    return;
                }

                ItemComposition item = f2pItems.get(random.nextInt(f2pItems.size()));
                ItemStats stats = Microbot.getItemManager().getItemStats(item.getId());
                if (stats == null) return;
                int limit = stats.getGeLimit();
                int count = bought.getOrDefault(item.getId(), 0);
                if (limit > 0 && count >= limit) return;
                if (limit > 0 && count >= limit - 1) return;

                if (config.useTradeVolume() && Rs2GrandExchange.getBuyingVolume(item.getId()) < 1000) return;

                int price = Rs2GrandExchange.getPrice(item.getId());
                if (price <= 0) return;
                int buyPrice = (int) (price * 0.95);
                boolean placed = Rs2GrandExchange.buyItem(item.getName(), buyPrice, 1);
                if (placed) {
                    offerTimes.put(Rs2GrandExchange.getAvailableSlot().getLeft(), Instant.now());
                    bought.put(item.getId(), count + 1);
                    int sellPrice = (int) (price * 1.05);
                    if (Rs2GrandExchange.hasFinishedBuyingOffers()) {
                        Rs2GrandExchange.collectToInventory();
                        if (Rs2GrandExchange.sellItem(item.getName(), 1, sellPrice)) {
                            profit += sellPrice - buyPrice;
                        }
                    }
                }

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void loadF2P() {
        Microbot.getClientThread().runOnClientThread(() -> {
            for (int id = 0; id <= 30890; id++) {
                ItemComposition ic = Microbot.getItemManager().getItemComposition(id);
                if (ic != null && !ic.isMembers() && ic.isTradeable()) {
                    f2pItems.add(ic);
                }
            }
            return null;
        });
    }

    private void cancelOldOffers() {
        Instant now = Instant.now();
        for (GrandExchangeSlots slot : GrandExchangeSlots.values()) {
            if (!Rs2GrandExchange.isSlotAvailable(slot)) {
                Instant time = offerTimes.get(slot);
                if (time != null && Duration.between(time, now).toMinutes() >= config.cancelAfterMinutes()) {
                    Rs2GrandExchange.abortAllOffers(true);
                    offerTimes.clear();
                    break;
                }
            }
        }
    }
}
