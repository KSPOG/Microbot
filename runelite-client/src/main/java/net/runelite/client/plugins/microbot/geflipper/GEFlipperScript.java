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
    }

    private final List<Offer> offers = new ArrayList<>();
    private final Queue<String> itemQueue = new LinkedList<>();
    private int startingGp;

    private long startTime;
    private final Rs2ItemManager itemManager = new Rs2ItemManager();
    private GEFlipperConfig config;
    private List<String> items = new ArrayList<>();

    private enum State {BUY, WAIT_BUY, SELL, WAIT_SELL}
    private State state = State.BUY;

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

                switch (state) {
                    case BUY:
                        status = "Buying";
                        if (offers.size() >= MAX_SLOTS) {
                            state = State.WAIT_BUY;
                            break;
                        }
                        String name = nextItem();
                        int id = itemManager.getItemId(name);
                        if (id <= 0) {
                            status = "Item not found";
                            break;
                        }
                        int buyPrice = Rs2GrandExchange.getOfferPrice(id);
                        int sellPrice = Rs2GrandExchange.getSellPrice(id);
                        if (buyPrice <= 0 || sellPrice <= 0) {
                            status = "Price lookup failed";
                            break;
                        }
                        int m = sellPrice - buyPrice;
                        if (m < conf.minMargin()) {
                            status = "Margin too low";
                            break;
                        }
                        if (Rs2Inventory.itemQuantity(ItemID.COINS_995) < buyPrice) {
                            status = "Not enough gp";
                            break;
                        }
                        boolean placed = Rs2GrandExchange.buyItem(name, buyPrice, 1);
                        if (!placed) {
                            status = "Unable to buy";
                            break;
                        }
                        Offer offer = new Offer();
                        offer.name = name;
                        offer.buyPrice = buyPrice;
                        offer.sellPrice = sellPrice;
                        offers.add(offer);
                        if (offers.size() >= MAX_SLOTS) {
                            state = State.WAIT_BUY;
                        }
                        break;
                    case WAIT_BUY:
                        status = "Waiting buy";
                        if (Rs2GrandExchange.hasFinishedBuyingOffers()) {
                            Rs2GrandExchange.collect(false);
                            state = State.SELL;
                        }
                        break;
                    case SELL:
                        status = "Selling";
                        for (Offer o : offers) {
                            Rs2GrandExchange.sellItem(o.name, 1, o.sellPrice);
                        }
                        state = State.WAIT_SELL;
                        break;
                    case WAIT_SELL:
                        status = "Waiting sell";
                        if (Rs2GrandExchange.hasFinishedSellingOffers()) {
                            Rs2GrandExchange.collect(true);
                            offers.clear();
                            state = State.BUY;
                        }
                        break;
                    default:
                        break;
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
