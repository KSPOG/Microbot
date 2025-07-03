package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

@Singleton
public class RandomTrainerScript extends Script {
    public static final String VERSION = "1.0.0";

    private RandomTrainerConfig config;
    private RandomTrainerPlugin plugin;
    private SkillTask currentTask;
    private long nextSwitch;
    private final Random random = new Random();
    // flag to avoid clicking a rock multiple times before mining starts
    private boolean waitingForAnim = false;
    private long animWaitStart = 0L;
    private boolean idleForBreak = false;

    public boolean run(RandomTrainerConfig config, RandomTrainerPlugin plugin) {
        if (isRunning()) {
            return false; // prevent multiple schedules which could freeze the client
        }

        this.config = config;
        this.plugin = plugin;

        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.takeMicroBreaks = true;

        nextSwitch = System.currentTimeMillis() + config.switchDelay() * 60_000L;
        Microbot.status = "Selecting task";
        selectNewTask();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::loop, 0, 1, TimeUnit.SECONDS);
        return true;
    }

    public SkillTask getCurrentTask() {
        return currentTask;
    }

    public String getCurrentTaskName() {
        if (currentTask == null) {
            return "None";
        }
        String n = currentTask.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
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
        // Only choose among skills that have an implementation. All other
        // tasks are placeholders and would leave the bot idling.
        SkillTask[] available = {
                SkillTask.MINING
        };

        SkillTask newTask;
        do {
            newTask = available[random.nextInt(available.length)];
        } while (newTask == currentTask);

        currentTask = newTask;
        Microbot.status = "Idle";
    }

    private void executeCurrentTask() {
        switch (currentTask) {
            case MINING:
                Microbot.status = "Mining";
                trainLowLevelMining();
                break;
            default:
                Microbot.status = "Idle";
                break;
        }
    }

    private void stopCurrentTask() {
        switch (currentTask) {
            case MINING:
                break;
            default:
                break;
        }
    }

    private void trainLowLevelMining() {
        if (!ensurePickaxe()) {
            Microbot.status = "Getting pickaxe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking ore";
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll("tin ore");
                Rs2Bank.depositAll("copper ore");
            }
            return;
        }

        WorldPoint mine = new WorldPoint(3288, 3363, 0);
        if (Rs2Player.getWorldLocation().distanceTo(mine) > 5) {
            Microbot.status = "Walking to mine";
            Rs2Walker.walkTo(mine);
            return;
        }
        // if we've clicked a rock and the animation hasn't started yet, wait
        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false; // animation started
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                // nothing happened for 5s, try again
                waitingForAnim = false;
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Mining";
            return; // wait until mining animation has finished
        }

        int tinCount = Rs2Inventory.itemQuantity("tin ore");
        int copperCount = Rs2Inventory.itemQuantity("copper ore");
        String rockName = tinCount <= copperCount ? "Tin rocks" : "Copper rocks";

        GameObject rock = Rs2GameObject.findReachableObject(rockName, true, 10, mine);
        if (rock != null && Rs2GameObject.interact(rock)) {
            Microbot.status = "Mining";
            waitingForAnim = true; // avoid spam clicking until animation begins
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.takeMicroBreakByChance();
        }
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
            Microbot.status = "Walking to bank";
            Rs2Bank.walkToBankAndUseBank();
            return false;
        }

        int level = Rs2Player.getRealSkillLevel(Skill.MINING);
        String[] pickaxes = {
                "Rune pickaxe",
                "Adamant pickaxe",
                "Mithril pickaxe",
                "Black pickaxe",
                "Steel pickaxe",
                "Iron pickaxe",
                "Bronze pickaxe"
        };
        int[] requirements = {41, 31, 21, 11, 6, 1, 1};
        String chosen = null;
        for (int i = 0; i < pickaxes.length; i++) {
            if (level >= requirements[i] && Rs2Bank.hasItem(pickaxes[i])) {
                Microbot.status = "Withdrawing pickaxe";
                Rs2Bank.withdrawItem(true, pickaxes[i]);
                chosen = pickaxes[i];
                break;
            }
        }
        Rs2Bank.closeBank();
        if (chosen != null) {
            Rs2Inventory.interact(chosen, "Wield");
        }
        return Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("pickaxe"))
                || Rs2Inventory.hasItem("pickaxe");
    }

    // No-op placeholders retained for future plugin integration
    private void startPlugin(Class<? extends Plugin> clazz) { }

    private void stopPlugin(Class<? extends Plugin> clazz) { }

}
