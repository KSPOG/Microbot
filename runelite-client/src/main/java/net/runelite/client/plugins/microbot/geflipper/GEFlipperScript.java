package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.item.Rs2ItemManager;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {

    public static final String VERSION = "1.0";
    public static int profit = 0;
    public static int profitPerHour = 0;
    // legacy fields retained for possible future use
    // but not currently required by the flipper logic

    private static final int MAX_SLOTS = 3;

    private static class Offer {
        String name;
        int buy;
        int sell;
    }

    private final List<Offer> offers = new ArrayList<>();
    private int startingGp;
    public static int margin = 0;
    public static int buyPrice = 0;
    public static int sellPrice = 0;


    private long startTime;
    private final Rs2ItemManager itemManager = new Rs2ItemManager();
    private GEFlipperConfig config;
    private List<String> items = new ArrayList<>();
    private int currentIndex = 0;

    private enum State {BUY, WAIT_BUY, SELL, WAIT_SELL}
    private State state = State.BUY;

    public boolean run(GEFlipperConfig config) {
        this.config = config;
        final GEFlipperConfig conf = this.config;
        items = getTradeableF2PItems();
        currentIndex = 0;
        startingGp = Rs2Inventory.itemQuantity(ItemID.COINS_995);
        startTime = System.currentTimeMillis();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (items.isEmpty()) return;

                switch (state) {
                    case BUY:
                        while (offers.size() < MAX_SLOTS) {
                            String name = items.get(currentIndex);
                            int id = itemManager.getItemId(name);
                            if (id <= 0) {
                                nextItem();
                                continue;
                            }
                            int buy = Rs2GrandExchange.getOfferPrice(id);
                            int sell = Rs2GrandExchange.getSellPrice(id);
                            int m = sell - buy;
                            if (m < conf.minMargin()) {
                                nextItem();
                                continue;
                            }
                            if (Rs2Inventory.itemQuantity(ItemID.COINS_995) < buy) {
                                break;
                            }
                            Rs2GrandExchange.buyItem(name, buy, 1);
                            Offer offer = new Offer();
                            offer.name = name;
                            offer.buy = buy;
                            offer.sell = sell;
                            offers.add(offer);
                            nextItem();
                        }
                        if (offers.size() == MAX_SLOTS) {
                            state = State.WAIT_BUY;
                        }
                        break;
                    case WAIT_BUY:
                        if (Rs2GrandExchange.hasFinishedBuyingOffers()) {

                String itemName = config.itemName();
                int itemId = itemManager.getItemId(itemName);
                buyPrice = Rs2GrandExchange.getOfferPrice(itemId);
                sellPrice = Rs2GrandExchange.getSellPrice(itemId);
                margin = sellPrice - buyPrice;

                switch (state) {
                    case BUY:
                        Rs2GrandExchange.buyItem(itemName, buyPrice, 1);
                        state = State.WAIT_BUY;
                        break;
                    case WAIT_BUY:
                        if (Rs2GrandExchange.hasBoughtOffer()) {
                            Rs2GrandExchange.collect(false);
                            state = State.SELL;
                        }
                        break;
                    case SELL:
                        for (Offer offer : offers) {
                            Rs2GrandExchange.sellItem(offer.name, 1, offer.sell);
                        }
                        state = State.WAIT_SELL;
                        break;
                    case WAIT_SELL:
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

                        Rs2GrandExchange.sellItem(itemName, 1, sellPrice);
                        state = State.WAIT_SELL;
                        break;
                    case WAIT_SELL:
                        if (Rs2GrandExchange.hasSoldOffer()) {
                            Rs2GrandExchange.collect(true);
                            profit += margin;

                            state = State.BUY;
                        }
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
        profit = 0;
        profitPerHour = 0;
    }

    private void nextItem() {
        currentIndex++;
        if (currentIndex >= items.size()) {
            currentIndex = 0;
        }
        margin = 0;

    }

    public List<String> getTradeableF2PItems() {
        List<String> items = new ArrayList<>();
        int count = Microbot.getClient().getItemCount();
        for (int id = 0; id < count; id++) {
            final int itemId = id;
            ItemComposition comp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getItemManager().getItemComposition(itemId)).orElse(null);
            if (comp != null && comp.isTradeable() && !comp.isMembers()) {
                items.add(comp.getName());
            }
        }
        return items;
    }
}
