package net.runelite.client.plugins.microbot.util.grandexchange;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.MenuAction;
import net.runelite.api.VarClientStr;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.*;

public class Rs2GrandExchange {

    public static final int GRAND_EXCHANGE_OFFER_CONTAINER_QTY_10 = 30474265;
    public static final int GRAND_EXCHANGE_OFFER_CONTAINER_QTY_100 = 30474265;
    public static final int GRAND_EXCHANGE_OFFER_CONTAINER_QTY_1000 = 30474265;
    public static final int GRAND_EXCHANGE_OFFER_CONTAINER_QTY_X = 30474265;
    public static final int GRAND_EXCHANGE_OFFER_CONTAINER_QTY_1 = 30474265;
    public static final int COLLECT_BUTTON = 30474246;
    private static final String GE_TRACKER_API_URL = "https://www.ge-tracker.com/api/items/";
    private static String geTrackerKey = "";

    public static void setGeTrackerKey(String key) {
        geTrackerKey = key == null ? "" : key;
    }

    /**
     * close the grand exchange interface
     */
    public static void closeExchange() {
        Microbot.status = "Closing Grand Exchange";
        if (!isOpen()) return;
        Rs2Widget.clickChildWidget(30474242, 11);
        sleepUntilOnClientThread(() -> Rs2Widget.getWidget(30474242) == null);
    }

    /**
     * Back button. Goes back from buy/sell offer screen to all slots overview.
     */
    public static void backToOverview() {
        Microbot.status = "Back to overview";
        if (!isOpen() && !isOfferScreenOpen()) return;
        Rs2Widget.clickWidget(30474244);
        sleepUntilOnClientThread(() -> !isOfferScreenOpen());
    }

    /**
     * check if the grand exchange screen is open
     *
     * @return
     */
    public static boolean isOpen() {
        return !Microbot.getClientThread().runOnClientThreadOptional(() -> Rs2Widget.getWidget(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER) == null
                || Rs2Widget.getWidget(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER).isHidden()).orElse(false);
    }

    /**
     * check if the ge offerscreen is open
     *
     * @return
     */
    public static boolean isOfferScreenOpen() {
        Microbot.status = "Checking if Offer is open";
        return Rs2Widget.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER) != null;
    }

    /**
     * Opens the grand exchange
     *
     * @return
     */
    public static boolean openExchange() {
        Microbot.status = "Opening Grand Exchange";
        try {
            if (Rs2Inventory.isItemSelected())
                Microbot.getMouse().click();
            if (isOpen()) return true;
            Rs2NpcModel npc = Rs2Npc.getNpc("Grand Exchange Clerk");
            if (npc == null) return false;
            Rs2Npc.interact(npc, "exchange");
            sleepUntil(Rs2GrandExchange::isOpen, 5000);
            return isOpen();
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2GrandExchange", ex);
        }
        return false;
    }

    /**
     * @param itemName
     * @param price
     * @param quantity
     * @return true if item has been bought succesfully
     */
    public static boolean buyItem(String itemName, int price, int quantity) {
        return buyItem(itemName, itemName, price, quantity);
    }

    /**
     * @param itemName   name of the item
     * @param searchTerm search term
     * @param price      price of the item to buy
     * @param quantity   quantity of item to buy
     * @return true if item has been bought succesfully
     */
    public static boolean buyItem(String itemName, String searchTerm, int price, int quantity) {
        try {
            if (useGrandExchange()) return false;

            Pair<GrandExchangeSlots, Integer> slot = getAvailableSlot();
            if (slot.getLeft() == null) {
                if (hasBoughtOffer()) {
                    collectToBank();
                }
                return false;
            }
            Widget buyOffer = getOfferBuyButton(slot.getLeft());
            if (buyOffer == null) return false;

            Rs2Widget.clickWidgetFast(buyOffer);
            sleepUntil(Rs2GrandExchange::isOfferTextVisible, 5000);
            sleepUntil(() -> Rs2Widget.hasWidget("What would you like to buy?"));
            Rs2Keyboard.typeString(searchTerm);
            sleepUntil(() -> !Rs2Widget.hasWidget("Start typing the name"), 5000); //GE Search Results
            sleep(1200);
            Pair<Widget, Integer> itemResult = getSearchResultWidget(itemName);
            if (itemResult != null) {
                Rs2Widget.clickWidgetFast(itemResult.getLeft(), itemResult.getRight(), 1);
                sleepUntil(() -> getPricePerItemButton_X() != null);
            }
            Widget pricePerItemButtonX = getPricePerItemButton_X();
            if (pricePerItemButtonX != null) {


                setPrice(price);
                setQuantity(quantity);
                if (getOfferPrice() == price && getOfferQuantity() == quantity) {
                    confirm();
                    return true;
                } else {
                    return buyItem(itemName, searchTerm, price, quantity);
                }

            } else {
                System.out.println("unable to find widget setprice.");
            }

            return false;
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2GrandExchange", ex);
        }
        return false;
    }

    private static void confirm() {
        Microbot.getMouse().click(getConfirm().getBounds());
        sleepUntil(() -> Rs2Widget.hasWidget("Your offer is much higher"), 2000);
        if (Rs2Widget.hasWidget("Your offer is much higher")) {
            Rs2Widget.clickWidget("Yes");
        }
    }

    private static void setQuantity(int quantity) {
        if (quantity != getOfferQuantity()) {
            Widget quantityButtonX = getQuantityButton_X();
            Microbot.getMouse().click(quantityButtonX.getBounds());
            sleepUntil(() -> Rs2Widget.getWidget(InterfaceID.Chatbox.MES_TEXT2) != null); //GE Enter Price/Quantity
            sleep(600, 1000);
            setChatboxValue(quantity);
            sleep(500, 750);
            Rs2Keyboard.enter();
            sleep(1000);
        }
    }
    private static void setPrice(int price) {
        if (price != getOfferPrice()) {
            Widget pricePerItemButtonX = getPricePerItemButton_X();
            Microbot.getMouse().click(pricePerItemButtonX.getBounds());
            sleepUntil(() -> Rs2Widget.getWidget(InterfaceID.Chatbox.MES_TEXT2) != null); //GE Enter Price
            sleep(600, 1000);
            setChatboxValue(price);
            sleep(500, 750);
            Rs2Keyboard.enter();
            sleep(1000);
        }

    }


    /**
     * Buys item from the grand exchange 5% above the average price
     *
     * @param itemName
     * @param quantity
     * @param timesToIncreasePrice the amount to click +5% price increase
     * @return
     */

    private static boolean useGrandExchange() {
        if (!isOpen()) {
            boolean hasExchangeOpen = openExchange();
            if (!hasExchangeOpen) {
                boolean isAtGe = walkToGrandExchange();
                return !isAtGe;
            }
        }
        return false;
    }

    /**
     * Sell item to the grand exchange
     *
     * @param itemName name of the item to sell
     * @param quantity quantity of the item to sell
     * @param price    price of the item to sell
     * @return
     */
    public static boolean sellItem(String itemName, int quantity, int price) {
        try {
            if (!Rs2Inventory.hasItem(itemName)) return false;

            if (useGrandExchange()) return false;

            Pair<GrandExchangeSlots, Integer> slot = getAvailableSlot();
            Widget sellOffer = getOfferSellButton(slot.getLeft());

            if (sellOffer == null) return false;

            Microbot.getMouse().click(sellOffer.getBounds());
            sleepUntil(Rs2GrandExchange::isOfferTextVisible, 5000);
            Rs2Inventory.interact(itemName, "Offer");
            sleepUntil(() -> Rs2Widget.hasWidget("actively traded price"));
            sleep(300, 600);
            Widget pricePerItemButtonX = getPricePerItemButton_X();
            if (pricePerItemButtonX != null) {
                setPrice(price);
                setQuantity(quantity);
                if (getOfferPrice() == price && getOfferQuantity() == quantity) {
                    confirm();
                    return true;
                } else {
                    return sellItem(itemName, quantity, price);
                }
            } else {
                System.out.println("unable to find widget setprice.");
            }
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2GrandExchange", ex);
        }
        return false;
    }


    /**
     * Collect all the grand exchange slots to the bank or inventory
     *
     * @param collectToBank
     * @return
     */
    public static boolean collect(boolean collectToBank) {
        if (isAllSlotsEmpty()) {
            return true;
        }
        if (Rs2Inventory.isFull()) {
            if (Rs2Bank.useBank()) {
                Rs2Bank.depositAll();
            }
        }
        if (!isOpen()) {
            openExchange();
        }
        sleepUntil(Rs2GrandExchange::isOpen);
        Widget[] collectButton = Rs2Widget.getWidget(465, 6).getDynamicChildren();
        if (!collectButton[1].isSelfHidden()) {
            Rs2Widget.clickWidgetFast(
                    COLLECT_BUTTON, collectToBank ? 2 : 1);
            sleepUntil(() -> collectButton[1].isSelfHidden());
        }
        return collectButton[1].isSelfHidden();
    }

    public static boolean collectToInventory() {
        return collect(false);
    }

    /**
     * Collect all the grand exchange items to your bank
     *
     * @return
     */
    public static boolean collectToBank() {
        return collect(true);
    }

    /**
     * sells all the tradeable loot items from a specific npc name
     *
     * @param npcName
     * @return true if there is no more loot to sell
     */
    public static boolean sellLoot(String npcName, List<String> itemsToNotSell) {

        boolean soldAllItems = Rs2Bank.withdrawLootItems(npcName, itemsToNotSell);

        if (soldAllItems) {
            boolean isSuccess = sellInventory();

            return isSuccess;
        }


        return false;
    }

    /**
     * Sells all the tradeable items in your inventory
     *
     * @return
     */
    public static boolean sellInventory() {
        for (Rs2ItemModel item : Rs2Inventory.items()) {

            if (!item.isTradeable()) continue;

            if (Rs2GrandExchange.getAvailableSlot().getKey() == null && Rs2GrandExchange.hasSoldOffer()) {
                Rs2GrandExchange.collectToBank();
                sleep(600);
            }

            int price = getOfferPrice(item.getId());
            sellItem(item.getName(), item.getQuantity(), price);
        }
        return Rs2Inventory.isEmpty();
    }

    /**
     * Aborts the offer
     *
     * @param name          name of the item to abort offer on
     * @param collectToBank collect the item to the bank
     * @return true if the offer has been aborted
     */
    public static boolean abortOffer(String name, boolean collectToBank) {
        if (useGrandExchange()) return false;
        try {
            for (GrandExchangeSlots slot : GrandExchangeSlots.values()) {
                Widget parent = getSlot(slot);
                if (parent == null) continue;
                if (isSlotAvailable(slot)) continue; // skip if slot is empty
                Widget child = parent.getChild(19);
                if (child == null) continue;
                if (child.getText().equalsIgnoreCase(name)) {
                    Microbot.doInvoke(new NewMenuEntry("Abort offer", 2, parent.getId(), MenuAction.CC_OP.getId(), 2, -1, ""), new Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
                    sleep(1000);
                    collect(collectToBank);
                    return true;
                }
            }
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2GrandExchange", ex);
        }
        return false;
    }

    /**
     * Aborts all offers
     *
     * @param collectToBank collect the items to the bank
     * @return true if all the offers have been aborted
     */
    public static boolean abortAllOffers(boolean collectToBank) {
        if (useGrandExchange()) return false;
        try {
            for (GrandExchangeSlots slot : GrandExchangeSlots.values()) {
                Widget parent = getSlot(slot);
                if (parent == null) continue;
                if (isSlotAvailable(slot)) continue; // skip if slot is empty
                Widget child = parent.getChild(19);
                if (child == null) continue;
                Microbot.doInvoke(new NewMenuEntry("Abort offer", 2, parent.getId(), MenuAction.CC_OP.getId(), 2, -1, ""), new Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
            }
            sleep(1000);
            collect(collectToBank);
            return isAllSlotsEmpty();
        } catch (Exception ex) {
            Microbot.logStackTrace("Rs2GrandExchange", ex);
            return false;
        }
    }

    public static Pair<Widget, Integer> getSearchResultWidget(String search) {
        Widget parent = Microbot.getClient().getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);

        if (parent == null || parent.getChildren() == null) return null;

        Widget child = Arrays.stream(parent.getChildren()).filter(x -> x.getText().equalsIgnoreCase(search)).findFirst().orElse(null);

        if (child != null) {
            List<Widget> children = Arrays.stream(parent.getChildren()).collect(Collectors.toList());
            int index = children.indexOf(child);
            int originalWidgetIndex = index - 1;
            return Pair.of(children.get(originalWidgetIndex), originalWidgetIndex);
        }
        return null;
    }

    public static Pair<Widget, Integer> getSearchResultWidget(int itemId) {
        Widget parent = Microbot.getClient().getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS);

        if (parent == null || parent.getChildren() == null) return null;

        Widget child = Arrays.stream(parent.getChildren()).filter(x -> x.getItemId() == itemId).findFirst().orElse(null);

        if (child != null) {
            List<Widget> children = Arrays.stream(parent.getChildren()).collect(Collectors.toList());
            int index = children.indexOf(child);
            int originalWidgetIndex = index - 2;
            return Pair.of(children.get(originalWidgetIndex), originalWidgetIndex);
        }

        return null;
    }

    private static Widget getOfferContainer() {
        return Microbot.getClient().getWidget(InterfaceID.GE_OFFERS,26);
    }

    public static Widget getQuantityButton_Minus() {

        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(1)).orElse(null);
    }

    public static Widget getQuantityButton_Plus() {

        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(2)).orElse(null);
    }

    public static Widget getQuantityButton_1() {

        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(3)).orElse(null);
    }

    public static Widget getQuantityButton_10() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(4)).orElse(null);
    }

    public static Widget getQuantityButton_100() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(5)).orElse(null);
    }

    public static Widget getQuantityButton_1000() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(6)).orElse(null);
    }

    public static Widget getQuantityButton_X() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(7)).orElse(null);
    }

    public static Widget getPricePerItemButton_Minus() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(8)).orElse(null);
    }

    public static Widget getPricePerItemButton_Plus() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(9)).orElse(null);
    }

    public static Widget getPricePerItemButton_Minus_5Percent() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(10)).orElse(null);
    }

    public static Widget getPricePerItemButton_GuidePrice() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(11)).orElse(null);
    }

    public static Widget getPricePerItemButton_X() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(12)).orElse(null);
    }

    public static Widget getPricePerItemButton_Plus5Percent() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(13)).orElse(null);
    }

    public static Widget getPricePerItemButton_PlusXPercent() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(15)).orElse(null);
    }

    public static Widget getChooseItem() {
        Widget parent = getOfferContainer();

        return Optional.ofNullable(parent).map(p -> p.getChild(20)).orElse(null);
    }

    public static Widget getConfirm() {
        Widget parent = getOfferContainer();

        return Rs2Widget.findWidget("Confirm", Arrays.stream(parent.getDynamicChildren()).collect(Collectors.toList()), true);
    }

    public static boolean isOfferTextVisible() {
        return Rs2Widget.isWidgetVisible(ComponentID.GRAND_EXCHANGE_OFFER_DESCRIPTION);
    }

    private static boolean hasOfferPriceChanged(int basePrice) {
        return basePrice != getItemPrice();
    }

    public static Widget getItemPriceWidget() {
        return Rs2Widget.getWidget(465, 27);
    }

    public static int getItemPrice() {
        return Integer.parseInt(Rs2Widget.getWidget(465, 27).getText().replace(" coins", ""));
    }

    public static Widget getSlot(GrandExchangeSlots slot) {
        switch (slot) {
            case ONE:
                return Rs2Widget.getWidget(465, 7);
            case TWO:
                return Rs2Widget.getWidget(465, 8);
            case THREE:
                return Rs2Widget.getWidget(465, 9);
            case FOUR:
                return Rs2Widget.getWidget(465, 10);
            case FIVE:
                return Rs2Widget.getWidget(465, 11);
            case SIX:
                return Rs2Widget.getWidget(465, 12);
            case SEVEN:
                return Rs2Widget.getWidget(465, 13);
            case EIGHT:
                return Rs2Widget.getWidget(465, 14);
            default:
                return null;
        }
    }

    public static boolean isSlotAvailable(GrandExchangeSlots slot) {
        Widget parent = getSlot(slot);
        return Optional.ofNullable(parent).map(p -> p.getChild(2).isSelfHidden()).orElse(false);
    }

    public static Widget getOfferBuyButton(GrandExchangeSlots slot) {
        Widget parent = getSlot(slot);
        return Optional.ofNullable(parent).map(p -> p.getChild(0)).orElse(null);
    }

    public static Widget getOfferSellButton(GrandExchangeSlots slot) {
        Widget parent = getSlot(slot);
        return Optional.ofNullable(parent).map(p -> p.getChild(1)).orElse(null);
    }

    public static Pair<GrandExchangeSlots, Integer> getAvailableSlot() {
        int maxSlots = getMaxSlots();
        int slotsAvailable = 0;
        GrandExchangeSlots availableSlot = null;
        for (int i = 0; i < maxSlots; i++) {
            GrandExchangeSlots slot = GrandExchangeSlots.values()[i];
            if (Rs2GrandExchange.isSlotAvailable(slot)) {
                if (availableSlot == null) {
                    availableSlot = slot;
                }
                slotsAvailable++;
            }
        }
        return Pair.of(availableSlot, slotsAvailable);
    }

    public static boolean isAllSlotsEmpty() {
        return getAvailableSlot().getRight() == Arrays.stream(GrandExchangeSlots.values()).count();
    }

    public static boolean hasBoughtOffer() {
        return Arrays.stream(Microbot.getClient().getGrandExchangeOffers()).anyMatch(x -> x.getState() == GrandExchangeOfferState.BOUGHT);
    }
    // check if all buy offers are bought, no state can be buying and at least one offer must be bought
    public static boolean hasFinishedBuyingOffers() {
        GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
        boolean hasBought = Arrays.stream(offers)
                .anyMatch(offer -> offer.getState() == GrandExchangeOfferState.BOUGHT);
        boolean isBuying = Arrays.stream(offers)
                .anyMatch(offer -> offer.getState() == GrandExchangeOfferState.BUYING);
        return hasBought && !isBuying;
    }

    public static boolean hasSoldOffer() {
        return Arrays.stream(Microbot.getClient().getGrandExchangeOffers()).anyMatch(x -> x.getState() == GrandExchangeOfferState.SOLD);
    }
    public static boolean hasFinishedSellingOffers() {
        GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
        boolean hasSold = Arrays.stream(offers)
                .anyMatch(offer -> offer.getState() == GrandExchangeOfferState.SOLD);
        boolean isSelling = Arrays.stream(offers)
                .anyMatch(offer -> offer.getState() == GrandExchangeOfferState.SELLING);
        return hasSold && !isSelling;
    }


    private static int getMaxSlots() {
        return Rs2Player.isMember() ? 8 : 3;
    }

    public static boolean walkToGrandExchange() {
        return Rs2Walker.walkTo(BankLocation.GRAND_EXCHANGE.getWorldPoint());
    }


    private static JsonObject requestItemData(int itemId) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(GE_TRACKER_API_URL + itemId));
        if (!geTrackerKey.isEmpty()) {
            builder.header("Key", geTrackerKey);
        }
        HttpRequest request = builder.build();
        try {
            String jsonResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JsonParser parser = new JsonParser();
            return parser.parse(new StringReader(jsonResponse)).getAsJsonObject().getAsJsonObject("data");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getOfferPrice(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buying")) {
            return -1;
        }
        return data.get("buying").getAsInt();
    }

    public static int getSellPrice(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("selling")) {






            return -1;
        }
        return data.get("selling").getAsInt();
    }

    public static int getPrice(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("overall")) {
            return -1;
        }
        return data.get("overall").getAsInt();
    }

    public static int getBuyingQuantity(int itemId) {

        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();
    }

    public static int getSellingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GE_TRACKER_API_URL + itemId))
                .build();

        try {
            String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .join();

            JsonParser parser = new JsonParser();
            JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
            JsonObject data = jsonElement.getAsJsonObject("data");

            return data.get("buyingQuantity").getAsInt();
        } catch (Exception e) {
            e.printStackTrace();


            return -1;
        }
        return data.get("selling").getAsInt();
    }


    public static int getPrice(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("overall")) {
            return -1;
        }
        return data.get("overall").getAsInt();
    }

    public static int getBuyingVolume(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();
    }

    public static int getSellingVolume(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {

            return -1;
        }
        return data.get("overall").getAsInt();
    }

    public static int getBuyingVolume(int itemId) {

            return -1;
        }
        return data.get("overall").getAsInt();
    }

    public static int getBuyingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();
    }

    public static int getSellingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {

            return -1;
        }
        return data.get("overall").getAsInt();
    }

    public static int getBuyingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();
    }

    public static int getSellingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {

            return -1;
        }
        return data.get("overall").getAsInt();
            return -1;
        }
        return data.get("overall").getAsInt();
    }


    public static int getBuyingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();
    }

    public static int getSellingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {


    public static int getSellingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {

    public static int getSellingQuantity(int itemId) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GE_TRACKER_API_URL + itemId))
                .build();

        try {
            String jsonResponse = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .join();

            JsonParser parser = new JsonParser();
            JsonObject jsonElement = parser.parse(new StringReader(jsonResponse)).getAsJsonObject();
            JsonObject data = jsonElement.getAsJsonObject("data");

            return data.get("sellingQuantity").getAsInt();
        } catch (Exception e) {
            e.printStackTrace();


    public static int getBuyingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();

    }

    public static int getBuyingQuantity(int itemId) {

        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("buyingQuantity")) {
            return -1;
        }
        return data.get("buyingQuantity").getAsInt();
    }

    public static int getSellingVolume(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {

    public static int getSellingQuantity(int itemId) {
        JsonObject data = requestItemData(itemId);
        if (data == null || !data.has("sellingQuantity")) {


            return -1;
        }
        return data.get("sellingQuantity").getAsInt();
    }




    static int getOfferQuantity() {
        return Microbot.getVarbitValue(4396);
    }

    static int getOfferPrice() {
        return Microbot.getVarbitValue(4398);
    }

    public static void setChatboxValue(int value) {
        Widget chatboxInputWidget = Rs2Widget.getWidget(InterfaceID.Chatbox.MES_TEXT2);
        if (chatboxInputWidget == null) return;
        chatboxInputWidget.setText(value + "*");
        Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Microbot.getClient().setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));
            return null;
        });

    }
}

