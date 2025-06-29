package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.item.Rs2ItemManager;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {

    public static final String VERSION = "1.4";
    public static String status = "";
    public static int profit = 0;
    public static int profitPerHour = 0;

    private static final int MAX_SLOTS = 3;
    private static final long TRADE_COOLDOWN = 4 * 60 * 60 * 1000L;
    private long buyTimeoutMs = 25 * 60 * 1000L;

    private static class Offer {
        String name;
        int buyPrice;
        int sellPrice;
        int quantity;
        boolean selling;
        long placedTime;
    }

    private final List<Offer> offers = new ArrayList<>();
    private final Queue<String> itemQueue = new LinkedList<>();
    private final Map<String, Long> lastTrade = new HashMap<>();

    public static long startTime;
    private final Rs2ItemManager itemManager = new Rs2ItemManager();
    private GEFlipperConfig config;
    private List<String> items = new ArrayList<>();


    public boolean run(GEFlipperConfig config) {
        if (isRunning()) {
            shutdown();
        }
        offers.clear();
        itemQueue.clear();
        lastTrade.clear();

        this.config = config;
        final GEFlipperConfig conf = this.config;
        buyTimeoutMs = conf.cancelMinutes() * 60L * 1000L;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.naturalMouse = true;
        status = "Starting";
        if (conf.itemName() != null && !conf.itemName().trim().isEmpty()) {
            items = new ArrayList<>();
            items.add(conf.itemName().trim());
        } else {
            items = getTradeableF2PItems();
        }
        itemQueue.clear();
        itemQueue.addAll(items);
        startTime = System.currentTimeMillis();
        profit = 0;
        profitPerHour = 0;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (items.isEmpty()) return;

                if (BreakHandlerScript.breakIn > 0 && BreakHandlerScript.breakIn <= 180) {
                    status = "Waiting for break";
                    if (Rs2GrandExchange.isOpen()) {
                        Rs2GrandExchange.closeExchange();
                    }
                    return;
                }

                if (!Rs2GrandExchange.isOpen()) {
                    Rs2GrandExchange.openExchange();
                }

                // collect finished offers
                if (Rs2GrandExchange.hasBoughtOffer()) {
                    status = "Collecting";
                    Rs2GrandExchange.collect(false);
                }
                if (Rs2GrandExchange.hasSoldOffer()) {
                    status = "Collecting";
                    Rs2GrandExchange.collect(true);
                    for (Iterator<Offer> it = offers.iterator(); it.hasNext();) {
                        Offer o = it.next();
                        if (o.selling && !Rs2Inventory.hasItem(o.name)) {
                            profit += (o.sellPrice - o.buyPrice) * o.quantity;
                            long runtime = System.currentTimeMillis() - startTime;
                            if (runtime > 0) {
                                profitPerHour = (int) (profit * 3600000L / runtime);
                            }
                            it.remove();
                        }
                    }
                }

                // cancel stale buy offers
                for (Offer o : new ArrayList<>(offers)) {
                    if (!o.selling && !Rs2Inventory.hasItem(o.name)) {
                        if (System.currentTimeMillis() - o.placedTime > buyTimeoutMs) {
                            status = "Cancelling";
                            Rs2GrandExchange.abortOffer(o.name, false);
                            offers.remove(o);
                            itemQueue.add(o.name);
                            lastTrade.remove(o.name);
                        }
                    }
                }

                // place sell offers for bought items
                for (Offer o : new ArrayList<>(offers)) {
                    if (!o.selling && Rs2Inventory.hasItem(o.name)) {
                        status = "Selling";
                        Rs2GrandExchange.sellItem(o.name, o.quantity, o.sellPrice);
                        o.selling = true;
                    }
                    if (o.selling && !Rs2Inventory.hasItem(o.name)) {
                        offers.remove(o);
                    }
                }

                // place buy offers if slots available
                if (offers.size() >= MAX_SLOTS) {
                    status = "Waiting";
                }
                int attempts = 0;
                boolean placedSomething = false;
                while (offers.size() < MAX_SLOTS && attempts < items.size()) {
                    attempts++;
                    String name = nextItem();
                    if (name == null) {
                        status = "All items on cooldown";
                        break;
                    }
                    int id = itemManager.getItemId(name);
                    if (id <= 0) {
                        status = "Item not found";
                        continue;
                    }
                    int buyPrice = Rs2GrandExchange.getOfferPrice(id);
                    int sellPrice = Rs2GrandExchange.getSellPrice(id);
                    if (buyPrice <= 0 || sellPrice <= 0) {
                        status = "Price lookup failed";
                        continue;
                    }
                    int buyVol = Rs2GrandExchange.getBuyingVolume(id);
                    int sellVol = Rs2GrandExchange.getSellingVolume(id);
                    int volume = Math.min(buyVol, sellVol);
                    if (volume > 0 && volume < conf.minVolume()) {
                        status = "Volume too low";
                        itemQueue.add(name);
                        continue;
                    }
                    int coins = Rs2Inventory.itemQuantity(ItemID.COINS_995);
                    if (coins < buyPrice) {
                        status = "Not enough gp";
                        itemQueue.add(name);
                        continue;
                    }
                    int quantity = coins / ((MAX_SLOTS - offers.size()) * buyPrice);
                    if (quantity <= 0) {
                        quantity = coins / buyPrice;
                    }
                    if (quantity <= 0) {
                        status = "Not enough gp";
                        continue;
                    }

                    // respect GE buy limits
                    int buyLimit = 0;
                    var stats = Microbot.getItemManager().getItemStats(id);
                    if (stats != null) {
                        buyLimit = stats.getGeLimit();
                    }
                    if (buyLimit > 0 && quantity > buyLimit) {
                        quantity = buyLimit;
                    }
                    boolean placed = Rs2GrandExchange.buyItem(name, buyPrice, quantity);
                    if (!placed) {
                        status = "Unable to buy";
                        break;
                    }
                    placedSomething = true;
                    lastTrade.put(name, System.currentTimeMillis());
                    Offer offer = new Offer();
                    offer.name = name;
                    offer.buyPrice = buyPrice;
                    offer.sellPrice = sellPrice;
                    offer.quantity = quantity;
                    offer.selling = false;
                    offer.placedTime = System.currentTimeMillis();
                    offers.add(offer);
                }
                if (!placedSomething) {
                    status = "Waiting";
                }


            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, conf.delay(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
        offers.clear();
        itemQueue.clear();
        lastTrade.clear();
        profit = 0;
        profitPerHour = 0;
        status = "Stopped";
    }

    private String nextItem() {
        if (items.isEmpty()) {
            return null;
        }
        int attempts = 0;
        long now = System.currentTimeMillis();
        while (attempts < items.size()) {
            if (itemQueue.isEmpty()) {
                itemQueue.addAll(items);
            }
            String name = itemQueue.poll();
            Long last = lastTrade.get(name);
            if (last != null && now - last < TRADE_COOLDOWN) {
                attempts++;
                continue;
            }
            // item cooldowns only based on trade history now
            return name;
        }
        return null;
    }

    public List<String> getTradeableF2PItems() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            List<String> names = new ArrayList<>();
            int count = Microbot.getClient().getItemCount();
            for (int id = 0; id < count; id++) {
                ItemComposition comp = Microbot.getItemManager().getItemComposition(id);
                if (comp != null && comp.isTradeable() && !comp.isMembers()) {
                    names.add(comp.getName());
                }
            }
            return names;
        }).orElse(new ArrayList<>());
    }
}