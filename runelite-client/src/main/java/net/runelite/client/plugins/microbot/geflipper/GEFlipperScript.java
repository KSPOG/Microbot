package net.runelite.client.plugins.microbot.geflipper;

import net.runelite.api.ItemComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.item.Rs2ItemManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GEFlipperScript extends Script {

    public static final String VERSION = "1.0";
    public static int profit = 0;
    public static int profitPerHour = 0;
    public static int margin = 0;
    public static int buyPrice = 0;
    public static int sellPrice = 0;

    private long startTime;
    private final Rs2ItemManager itemManager = new Rs2ItemManager();
    private GEFlipperConfig config;

    private enum State {BUY, WAIT_BUY, SELL, WAIT_SELL}
    private State state = State.BUY;

    public boolean run(GEFlipperConfig config) {
        this.config = config;
        startTime = System.currentTimeMillis();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

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
        margin = 0;
    }

    public List<String> getTradeableF2PItems() {
        List<String> items = new ArrayList<>();
        int count = Microbot.getClient().getItemCount();
        for (int id = 0; id < count; id++) {
            ItemComposition comp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                    Microbot.getItemManager().getItemComposition(id)).orElse(null);
            if (comp != null && comp.isTradeable() && !comp.isMembers()) {
                items.add(comp.getName());
            }
        }
        return items;
    }
}
