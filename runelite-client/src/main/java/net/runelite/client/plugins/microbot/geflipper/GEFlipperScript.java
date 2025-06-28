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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {

    public static final String VERSION = "1.2";
    public static int profit = 0;
    public static int profitPerHour = 0;
    public static String status = "";

    private static final int MAX_SLOTS = 3;

    private static class Offer {
        String name;
        int buyPrice;
        int sellPrice;
        int quantity;
        boolean selling;
    }

    private final List<Offer> offers = new ArrayList<>();
    private final Queue<String> itemQueue = new LinkedList<>();
    private int startingGp;

    private long startTime;
    private final Rs2ItemManager itemManager = new Rs2ItemManager();
    private GEFlipperConfig config;
    private List<String> items = new ArrayList<>();


    public boolean run(GEFlipperConfig config) {
        this.config = config;
        final GEFlipperConfig conf = this.config;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.naturalMouse = true;
        status = "Starting";
        items = getTradeableF2PItems();
        itemQueue.clear();
        itemQueue.addAll(items);
        startingGp = Rs2Inventory.itemQuantity(ItemID.COINS_995);
        startTime = System.currentTimeMillis();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (items.isEmpty()) return;

                // collect finished offers
                if (Rs2GrandExchange.hasBoughtOffer()) {
                    status = "Collecting";
                    Rs2GrandExchange.collect(false);
                }
                if (Rs2GrandExchange.hasSoldOffer()) {
                    status = "Collecting";
                    Rs2GrandExchange.collect(true);
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
                while (offers.size() < MAX_SLOTS) {
                    String name = nextItem();
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
                    int margin = sellPrice - buyPrice;
                    if (margin < conf.minMargin()) {
                        status = "Margin too low";
                        continue;
                    }
                    int buyVol = Rs2GrandExchange.getBuyingVolume(id);
                    int sellVol = Rs2GrandExchange.getSellingVolume(id);
                    int volume = Math.min(buyVol, sellVol);
                    if (volume > 0 && volume < conf.minVolume()) {
                        status = "Volume too low";
                        continue;
                    }
                    int coins = Rs2Inventory.itemQuantity(ItemID.COINS_995);
                    if (coins < buyPrice) {
                        status = "Not enough gp";
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
                    boolean placed = Rs2GrandExchange.buyItem(name, buyPrice, quantity);
                    if (!placed) {
                        status = "Unable to buy";
                        break;
                    }
                    Offer offer = new Offer();
                    offer.name = name;
                    offer.buyPrice = buyPrice;
                    offer.sellPrice = sellPrice;
                    offer.quantity = quantity;
                    offer.selling = false;
                    offers.add(offer);
                }

                profit = Rs2Inventory.itemQuantity(ItemID.COINS_995) - startingGp;
                long elapsed = System.currentTimeMillis() - startTime;
                profitPerHour = (int) (profit / (elapsed / 3600000.0));

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
        profit = 0;
        profitPerHour = 0;
        status = "Stopped";
    }

    private String nextItem() {
        if (itemQueue.isEmpty()) {
            itemQueue.addAll(items);
        }
        return itemQueue.poll();
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