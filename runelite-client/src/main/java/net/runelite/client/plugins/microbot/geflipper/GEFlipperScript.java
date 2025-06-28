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

    public static final String VERSION = "1.1";
    public static int profit = 0;
    public static int profitPerHour = 0;

    private static final int MAX_SLOTS = 3;

    private static class Offer {
        String name;
        int buy;
        int sell;
    }

    private final List<Offer> offers = new ArrayList<>();
    private int startingGp;

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
                        if (offers.size() >= MAX_SLOTS) {
                            state = State.WAIT_BUY;
                            break;
                        }
                        String name = items.get(currentIndex);
                        nextItem();
                        int id = itemManager.getItemId(name);
                        if (id <= 0) {
                            break;
                        }
                        int buy = Rs2GrandExchange.getOfferPrice(id);
                        int sell = Rs2GrandExchange.getSellPrice(id);
                        int m = sell - buy;
                        if (m < conf.minMargin()) {
                            break;
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
                        if (offers.size() >= MAX_SLOTS) {
                            state = State.WAIT_BUY;
                        }
                        break;
                    case WAIT_BUY:
                        if (Rs2GrandExchange.hasFinishedBuyingOffers()) {
                            Rs2GrandExchange.collect(false);
                            state = State.SELL;
                        }
                        break;
                    case SELL:
                        for (Offer o : offers) {
                            Rs2GrandExchange.sellItem(o.name, 1, o.sell);
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
