package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.mining.AutoMiningPlugin;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Skill;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RandomTrainerScript extends Script {
    public static final String VERSION = "1.0.0";

    private RandomTrainerConfig config;
    private RandomTrainerPlugin plugin;
    private SkillTask currentTask;
    private long nextSwitch;
    private final Random random = new Random();
    private boolean idleForBreak = false;

    public boolean run(RandomTrainerConfig config, RandomTrainerPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        nextSwitch = System.currentTimeMillis() + config.switchDelay() * 60_000L;
        selectNewTask();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::loop, 0, 1, TimeUnit.SECONDS);
        return true;
    }

    public SkillTask getCurrentTask() {
        return currentTask;
    }

    private void loop() {
        try {
            if (!super.run() || !Microbot.isLoggedIn()) return;

            if (shouldIdleForBreak()) {
                handleUpcomingBreak();
                return;
            } else if (idleForBreak) {
                idleForBreak = false;
            }

            if (config.healAtHp() > 0) {
                int hp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
                if (hp <= config.healAtHp()) {
                    Rs2Player.useFood();
                }
            }

            if (System.currentTimeMillis() >= nextSwitch) {
                stopCurrentTask();
                selectNewTask();
                nextSwitch = System.currentTimeMillis() + config.switchDelay() * 60_000L;
            }

            executeCurrentTask();
        } catch (Exception ex) {
            Microbot.log(ex.getMessage());
        }
    }

    private boolean shouldIdleForBreak() {
        return plugin.isBreakHandlerEnabled() && BreakHandlerScript.breakIn > 0 && BreakHandlerScript.breakIn <= 180;
    }

    private void handleUpcomingBreak() {
        Microbot.status = "Break soon, idling at bank";
        if (!idleForBreak) {
            Rs2Bank.walkToBankAndUseBank();
            idleForBreak = true;
        }
        sleep(1000);
    }

    private void selectNewTask() {
        SkillTask[] tasks = SkillTask.values();
        SkillTask newTask;
        do {
            newTask = tasks[random.nextInt(tasks.length)];
        } while (newTask == currentTask);

        currentTask = newTask;
        Microbot.status = "Selected " + currentTask.name();
    }

    private void executeCurrentTask() {
        Microbot.status = "Training " + currentTask.name();
        switch (currentTask) {
            case MINING:
                if (Rs2Player.getRealSkillLevel(Skill.MINING) < 15) {
                    trainLowLevelMining();
                } else {
                    startPlugin(AutoMiningPlugin.class);
                }
                break;
            default:
                break;
        }
    }

    private void stopCurrentTask() {
        switch (currentTask) {
            case MINING:
                stopPlugin(AutoMiningPlugin.class);
                break;
            default:
                break;
        }
    }

    private void trainLowLevelMining() {
        if (!ensurePickaxe()) {
            return;
        }

        if (Rs2Inventory.isFull()) {
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll("tin ore");
                Rs2Bank.depositAll("copper ore");
            }
            return;
        }

        WorldPoint mine = new WorldPoint(3288, 3363, 0);
        if (Rs2Player.getWorldLocation().distanceTo(mine) > 5) {
            Rs2Walker.walkTo(mine);
            return;
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            return;
        }

        int tinCount = Rs2Inventory.itemQuantity("tin ore");
        int copperCount = Rs2Inventory.itemQuantity("copper ore");
        String rock = tinCount <= copperCount ? "Tin rocks" : "Copper rocks";
        Rs2GameObject.interact(rock, "Mine");
    }

    private boolean ensurePickaxe() {
        if (Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("pickaxe")) ||
                Rs2Inventory.hasItem("pickaxe")) {
            // attempt to wield if not equipped
            if (!Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("pickaxe"))) {
                Rs2Inventory.interact("pickaxe", "Wield");
            }
            return true;
        }

        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            return false;
        }

        int level = Rs2Player.getRealSkillLevel(Skill.MINING);
        String[] pickaxes = {"Black pickaxe", "Steel pickaxe", "Iron pickaxe", "Bronze pickaxe"};
        int[] requirements = {11, 6, 1, 1};
        for (int i = 0; i < pickaxes.length; i++) {
            if (level >= requirements[i] && Rs2Bank.hasItem(pickaxes[i])) {
                Rs2Bank.withdrawItem(true, pickaxes[i]);
                break;
            }
        }
        Rs2Bank.closeBank();
        return Rs2Inventory.hasItem("pickaxe");
    }

    private void startPlugin(Class<? extends Plugin> clazz) {
        Plugin p = Microbot.getPlugin(clazz.getName());
        if (p != null && !Microbot.isPluginEnabled(clazz)) {
            Microbot.startPlugin(p);
        }
    }

    private void stopPlugin(Class<? extends Plugin> clazz) {
        Plugin p = Microbot.getPlugin(clazz.getName());
        if (p != null && Microbot.isPluginEnabled(clazz)) {
            Microbot.stopPlugin(p);
        }
    }
}
