package net.runelite.client.plugins.microbot.progressivetrainer;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.mining.AutoMiningPlugin;
import net.runelite.client.plugins.microbot.mining.enums.Rocks;
import net.runelite.client.plugins.microbot.woodcutting.AutoWoodcuttingPlugin;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingResetOptions;
import net.runelite.client.plugins.microbot.woodcutting.enums.WoodcuttingTree;
import net.runelite.client.plugins.microbot.nateplugins.skilling.natefishing.AutoFishPlugin;
import net.runelite.client.plugins.microbot.nateplugins.skilling.natefishing.enums.Fish;
import net.runelite.client.plugins.microbot.cooking.AutoCookingPlugin;
import net.runelite.client.plugins.microbot.cooking.enums.CookingItem;
import net.runelite.client.plugins.microbot.cooking.enums.CookingLocation;
import net.runelite.client.plugins.microbot.sticktothescript.barbarianvillagefisher.BarbarianVillageFisherPlugin;
import net.runelite.client.plugins.microbot.sticktothescript.barbarianvillagefisher.enums.BarbarianFishingFunctions;
import net.runelite.client.plugins.microbot.sticktothescript.barbarianvillagefisher.enums.BarbarianFishingType;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterPlugin;
import net.runelite.client.plugins.microbot.bee.MossKiller.MossKillerPlugin;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.mining.shootingstar.enums.Pickaxe;
import net.runelite.client.plugins.microbot.TaF.DeadFallTrapHunter.Axe;
import net.runelite.api.ItemID;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import java.util.List;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ProgressiveTrainerScript extends Script {
    private ProgressiveTrainerConfig config;
    private Plugin currentPlugin;
    private long nextSwitch;
    private final Random random = new Random();
    private SkillTask currentTask;

    private enum SkillTask { MINING, WOODCUTTING, FISHING_COOKING, COMBAT }

    public String getCurrentTask() {
        return currentTask == null ? "" : currentTask.name();
    }

    public long getMinutesUntilSwitch() {
        long millisLeft = nextSwitch - System.currentTimeMillis();
        return millisLeft > 0 ? millisLeft / 60000 : 0;
    }

    public boolean run(ProgressiveTrainerConfig config) {
        if (isRunning()) return false;
        this.config = config;
        selectAndStartTask();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::loop, 1, 1, TimeUnit.SECONDS);
        return true;
    }

    private void loop() {
        try {
            if (!super.run() || !Microbot.isLoggedIn()) return;

            if (System.currentTimeMillis() >= nextSwitch) {
                stopCurrentPlugin();
                selectAndStartTask();
            }
        } catch (Exception ex) {
            Microbot.log(ex.getMessage());
        }
    }

    private void stopCurrentPlugin() {
        if (currentPlugin != null) {
            Microbot.stopPlugin(currentPlugin);
            currentPlugin = null;
        }
    }

    private void bankForTask(SkillTask task) {
        Rs2Bank.walkToBankAndUseBank();
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 10000);
        }
        Rs2Bank.depositAll();

        switch (task) {
            case MINING:
                Pickaxe pickaxe = getBestPickaxe(Stream.concat(Stream.concat(Rs2Inventory.items().stream(),
                        Rs2Equipment.items().stream()), Rs2Bank.bankItems().stream()).collect(Collectors.toList()));
                if (pickaxe != null && !Rs2Equipment.isWearing(pickaxe.getItemID()) && !Rs2Inventory.hasItem(pickaxe.getItemID())) {
                    Rs2Bank.withdrawItem(pickaxe.getItemID());
                }
                break;
            case WOODCUTTING:
                Axe axe = getBestAxe(Stream.concat(Stream.concat(Rs2Inventory.items().stream(),
                        Rs2Equipment.items().stream()), Rs2Bank.bankItems().stream()).collect(Collectors.toList()));
                if (axe != null && !Rs2Equipment.isWearing(axe.getItemID()) && !Rs2Inventory.hasItem(axe.getItemID())) {
                    Rs2Bank.withdrawItem(axe.getItemID());
                }
                Rs2Bank.withdrawItem(true, ItemID.TINDERBOX);
                break;
            case FISHING_COOKING:
                int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
                if (level < 20) {
                    Rs2Bank.withdrawItem(true, "small fishing net");
                } else {
                    Rs2Bank.withdrawItem(true, "fly fishing rod");
                    Rs2Bank.withdrawAll(true, "Feather");
                }
                break;
            case COMBAT:
                Rs2Bank.withdrawAll(true, "trout");
                break;
        }
        Rs2Bank.closeBank();
    }

    private void selectAndStartTask() {
        SkillTask[] tasks = SkillTask.values();
        SkillTask task = tasks[random.nextInt(tasks.length)];
        currentTask = task;
        bankForTask(task);
        switch (task) {
            case MINING:
                startMining();
                break;
            case WOODCUTTING:
                startWoodcutting();
                break;
            case FISHING_COOKING:
                startFishingCooking();
                break;
            case COMBAT:
                startCombat();
                break;
        }
        int min = config.minSwitchMinutes();
        int max = config.maxSwitchMinutes();
        int delay = min + random.nextInt(Math.max(1, max - min + 1));
        nextSwitch = System.currentTimeMillis() + delay * 60_000L;
    }

    private void startMining() {
        int level = Rs2Player.getRealSkillLevel(Skill.MINING);
        Rocks ore = Rocks.TIN;
        WorldPoint location = new WorldPoint(3232, 3152, 0); // Lumbridge mine
        if (level >= 15 && level < 30) {
            ore = Rocks.IRON;
            location = new WorldPoint(3292, 3353, 0); // Varrock east mine
        } else if (level >= 30 && level < 45) {
            ore = Rocks.IRON;
            location = new WorldPoint(3017, 3445, 0); // Dwarven mine
        } else if (level >= 45 && level < 60) {
            ore = Rocks.COAL;
            location = new WorldPoint(3017, 3445, 0);
        } else if (level >= 60 && level < 80) {
            ore = Rocks.MITHRIL;
            location = new WorldPoint(3030, 3347, 0); // Mining guild
        } else if (level >= 80) {
            ore = Rocks.ADAMANTITE;
            location = new WorldPoint(3030, 3347, 0);
        }
        Microbot.getConfigManager().setConfiguration("Mining", "Ore", ore);
        Microbot.getConfigManager().setConfiguration("Mining", "UseBank", false);
        Microbot.status = "Mining " + ore.name();
        Rs2Walker.walkTo(location);
        currentPlugin = Microbot.getPlugin(AutoMiningPlugin.class.getName());
        Microbot.startPlugin(currentPlugin);
    }

    private void startWoodcutting() {
        int level = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
        WoodcuttingTree tree = WoodcuttingTree.TREE;
        WorldPoint location = new WorldPoint(3222, 3218, 0); // Lumbridge
        if (level >= 15 && level < 30) {
            tree = WoodcuttingTree.OAK;
        } else if (level >= 30 && level < 45) {
            tree = WoodcuttingTree.WILLOW;
        } else if (level >= 45) {
            tree = WoodcuttingTree.MAPLE;
            location = new WorldPoint(2482, 2881, 0); // Corsair Cove Resource Area
        }
        Microbot.getConfigManager().setConfiguration("Woodcutting", "Tree", tree);
        Microbot.getConfigManager().setConfiguration("Woodcutting", "ItemAction", WoodcuttingResetOptions.FIREMAKE);
        Microbot.status = "Woodcutting " + tree.name();
        Rs2Walker.walkTo(location);
        currentPlugin = Microbot.getPlugin(AutoWoodcuttingPlugin.class.getName());
        Microbot.startPlugin(currentPlugin);
    }

    private void startFishingCooking() {
        int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
        if (level < 20) {
            // Lumbridge swamp fishing and cooking
            Microbot.getConfigManager().setConfiguration("micro-fishing", "Fish", Fish.SHRIMP);
            Microbot.getConfigManager().setConfiguration("autocooking", "itemToCook", CookingItem.RAW_SHRIMP);
            Microbot.getConfigManager().setConfiguration("autocooking", "cookingLocation", CookingLocation.LUMBRIDGE);
            Microbot.status = "Fishing shrimp";
            Rs2Walker.walkTo(new WorldPoint(3239, 3153, 0));
            currentPlugin = Microbot.getPlugin(AutoFishPlugin.class.getName());
            Microbot.startPlugin(currentPlugin);
            return;
        }
        // Barbarian village fishing & cooking
        Microbot.getConfigManager().setConfiguration("BarbarianVillageFisher", "fishType", BarbarianFishingType.FLY_FISHING);
        Microbot.getConfigManager().setConfiguration("BarbarianVillageFisher", "function", BarbarianFishingFunctions.COOK_AND_DROP);
        Microbot.status = "Fly fishing";
        Rs2Walker.walkTo(new WorldPoint(3108, 3432, 0));
        currentPlugin = Microbot.getPlugin(BarbarianVillageFisherPlugin.class.getName());
        Microbot.startPlugin(currentPlugin);
    }

    private void startCombat() {
        int level = Rs2Player.getRealSkillLevel(Skill.ATTACK);
        String npcs = "Chicken";
        WorldPoint location = new WorldPoint(3239, 3296, 0); // chickens
        Plugin plugin;
        if (level >= 15 && level < 30) {
            npcs = "Cow";
            location = new WorldPoint(3257, 3266, 0);
        } else if (level >= 30 && level < 50) {
            npcs = "Al-kharid warrior";
            location = new WorldPoint(3293, 3179, 0);
        } else if (level >= 50) {
            plugin = Microbot.getPlugin(MossKillerPlugin.class.getName());
            Rs2Walker.walkTo(new WorldPoint(3168, 9906, 0));
            currentPlugin = plugin;
            Microbot.startPlugin(plugin);
            return;
        }
        Microbot.getConfigManager().setConfiguration("PlayerAssistant", "monster", npcs);
        Microbot.getConfigManager().setConfiguration("PlayerAssistant", "toggleCombat", true);
        Microbot.getConfigManager().setConfiguration("PlayerAssistant", "centerLocation", location);
        Rs2Walker.walkTo(location);
        plugin = Microbot.getPlugin(AIOFighterPlugin.class.getName());
        currentPlugin = plugin;
        Microbot.status = "Fighting " + npcs;
        Microbot.startPlugin(plugin);
    }

    private Pickaxe getBestPickaxe(java.util.List<Rs2ItemModel> items) {
        Pickaxe best = null;
        for (Pickaxe p : Pickaxe.values()) {
            if (items.stream().noneMatch(i -> i.getName().toLowerCase().contains(p.getItemName()))) continue;
            if (p.hasRequirements()) {
                if (best == null || p.getMiningLevel() > best.getMiningLevel()) {
                    best = p;
                }
            }
        }
        return best;
    }

    private Axe getBestAxe(java.util.List<Rs2ItemModel> items) {
        Axe best = null;
        for (Axe a : Axe.values()) {
            if (items.stream().noneMatch(i -> i.getName().toLowerCase().contains(a.getItemName()))) continue;
            if (a.hasRequirements(true)) {
                if (best == null || a.getWoodcuttingLevel() > best.getWoodcuttingLevel()) {
                    best = a;
                }
            }
        }
        return best;
    }
}
