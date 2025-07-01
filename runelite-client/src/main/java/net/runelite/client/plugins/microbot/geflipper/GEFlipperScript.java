package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemStats;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.api.ItemID;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {
    private GEFlipperPlugin plugin;
    private GEFlipperConfig config;
    private final Random random = new Random();

    private final Map<Integer, Integer> bought = new HashMap<>();
    private final Map<Integer, Instant> firstBuyTime = new HashMap<>();
    private final Map<GrandExchangeSlots, Instant> offerTimes = new HashMap<>();
    private final Map<GrandExchangeSlots, Integer> slotItems = new HashMap<>();
    private final Map<GrandExchangeSlots, Integer> buyPrices = new HashMap<>();
    private final Map<GrandExchangeSlots, Integer> sellPrices = new HashMap<>();
    private final Deque<ItemComposition> itemQueue = new ArrayDeque<>();
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
        refillQueue();

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

                processSlots();

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

    private void refillQueue() {
        if (f2pItems.isEmpty()) return;
        Collections.shuffle(f2pItems, random);
        itemQueue.clear();
        itemQueue.addAll(f2pItems);
    }

    private void processSlots() {
        boolean actionTaken = false;
        boolean allBusy = true;
        for (int i = 0; i < 3; i++) {
            GrandExchangeSlots slot = GrandExchangeSlots.values()[i];
            GrandExchangeOffer offer = Microbot.getClient().getGrandExchangeOffers()[i];

            switch (offer.getState()) {
                case BOUGHT:
                    Microbot.status = "Collecting";
                    Rs2GrandExchange.collectToInventory();
                    ItemComposition boughtItem = Microbot.getItemManager().getItemComposition(slotItems.getOrDefault(slot, -1));
                    if (boughtItem != null) {
                        int sellPrice = sellPrices.getOrDefault(slot, 0);
                        if (sellHigh(boughtItem, sellPrice, 1)) {
                            Microbot.status = "Selling";
                        }
                    }
                    break;
                case SOLD:
                    Microbot.status = "Collecting";
                    Rs2GrandExchange.collectToInventory();
                    int profitAdd = sellPrices.getOrDefault(slot, 0) - buyPrices.getOrDefault(slot, 0);
                    profit += profitAdd;
                    slotItems.remove(slot);
                    buyPrices.remove(slot);
                    sellPrices.remove(slot);
                    offerTimes.remove(slot);
                    actionTaken = true;
                    break;
                case CANCELLED_BUY:
                case CANCELLED_SELL:
                    Microbot.status = "Collecting";
                    Rs2GrandExchange.collectToInventory();
                    slotItems.remove(slot);
                    buyPrices.remove(slot);
                    sellPrices.remove(slot);
                    offerTimes.remove(slot);
                    actionTaken = true;
                    break;
                case EMPTY:
                    slotItems.remove(slot);
                    buyPrices.remove(slot);
                    sellPrices.remove(slot);
                    offerTimes.remove(slot);
                    if (itemQueue.isEmpty()) {
                        refillQueue();
                    }
                    ItemComposition item = itemQueue.poll();
                    if (item == null) break;
                    ItemStats stats = Microbot.getItemManager().getItemStats(item.getId());
                    if (stats == null) break;
                    int limit = stats.getGeLimit();
                    int count = bought.getOrDefault(item.getId(), 0);
                    Instant first = firstBuyTime.get(item.getId());
                    if (first != null && Duration.between(first, Instant.now()).toHours() >= 4) {
                        count = 0;
                        bought.put(item.getId(), 0);
                        firstBuyTime.put(item.getId(), Instant.now());
                    }
                    if (limit > 0 && (count >= limit || count >= limit - 1)) {
                        break;
                    }
                    if (config.useTradeVolume() &&
                            Rs2GrandExchange.getBuyingVolume(item.getId()) < config.minimumTradeVolume()) {
                        break;
                    }
                    int price = Rs2GrandExchange.getPrice(item.getId());
                    if (price <= 0) break;
                    int buyPrice = (int) (price * 0.90); // buy low
                    int sellPrice = (int) (price * 1.10); // sell high
                    int coins = Rs2Inventory.itemQuantity(ItemID.COINS_995);
                    if (coins < buyPrice) break;
                    Microbot.status = "Buying";
                    if (buyLow(item, buyPrice, 1)) {
                        slotItems.put(slot, item.getId());
                        buyPrices.put(slot, buyPrice);
                        sellPrices.put(slot, sellPrice);
                        bought.put(item.getId(), count + 1);
                        firstBuyTime.putIfAbsent(item.getId(), Instant.now());
                        offerTimes.put(slot, Instant.now());
                        actionTaken = true;
                    }
                    break;
                default:
                    // BUYING or SELLING, do nothing
                    break;
            }
            if (offer.getState() == GrandExchangeOfferState.EMPTY) {
                allBusy = false;
            } else if (offer.getState() == GrandExchangeOfferState.BOUGHT || offer.getState() == GrandExchangeOfferState.SOLD) {
                allBusy = false;
            }
        }

        if (!actionTaken && allBusy) {
            Microbot.status = "Waiting";
        }
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

    private boolean buyLow(ItemComposition item, int price, int quantity) {
        return Rs2GrandExchange.buyItem(item.getName(), price, quantity);
    }

    private boolean sellHigh(ItemComposition item, int price, int quantity) {
        return Rs2GrandExchange.sellItem(item.getName(), quantity, price);
    }
}