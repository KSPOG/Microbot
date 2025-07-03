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
    private boolean waitingForAnim = false;
    private long animWaitStart = 0L;
    private boolean idleForBreak = false;

    private static final String[] PICKAXES = {
            "Rune pickaxe",
            "Adamant pickaxe",
            "Mithril pickaxe",
            "Black pickaxe",
            "Steel pickaxe",
            "Iron pickaxe",
            "Bronze pickaxe"
    };

    private static final int[] MINING_REQ = {41, 31, 21, 11, 6, 1, 1};
    private static final int[] ATTACK_REQ = {40, 30, 20, 10, 5, 1, 1};

    public boolean run(RandomTrainerConfig config, RandomTrainerPlugin plugin) {
        if (isRunning() || !Microbot.isLoggedIn()) {
            return false;
        }

        this.config = config;
        this.plugin = plugin;

        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.actionCooldownChance = 0.1;

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
        SkillTask[] available = { SkillTask.MINING };
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
                if (Rs2Player.getRealSkillLevel(Skill.MINING) < 15) {
                    trainLowLevelMining();
                } else {
                    trainIronMining();
                }
                break;
            default:
                Microbot.status = "Idle";
                break;
        }
    }

    private void stopCurrentTask() {
        // no-op for now
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

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Mining";
            return;
        }

        int tinCount = Rs2Inventory.itemQuantity("tin ore");
        int copperCount = Rs2Inventory.itemQuantity("copper ore");
        String rockName = tinCount <= copperCount ? "Tin rocks" : "Copper rocks";

        GameObject rock = Rs2GameObject.findReachableObject(rockName, true, 10, mine);
        if (rock != null && Rs2GameObject.interact(rock)) {
            Microbot.status = "Mining";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.actionCooldown();
        }
    }

    private void trainIronMining() {
        if (!ensurePickaxe()) {
            Microbot.status = "Getting pickaxe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking ore";
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll("iron ore");
                Rs2Bank.depositAll("copper ore");
                Rs2Bank.depositAll("tin ore");
            }
            return;
        }

        WorldPoint mine = new WorldPoint(3289, 3364, 0);
        if (Rs2Player.getWorldLocation().distanceTo(mine) > 5) {
            Microbot.status = "Walking to mine";
            Rs2Walker.walkTo(mine);
            return;
        }

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Mining";
            return;
        }

        GameObject rock = Rs2GameObject.findReachableObject("Iron rocks", true, 10, mine);
        if (rock != null && Rs2GameObject.interact(rock)) {
            Microbot.status = "Mining";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.actionCooldown();
        }
    }

    private boolean ensurePickaxe() {
        int miningLevel = Rs2Player.getRealSkillLevel(Skill.MINING);
        int attackLevel = Rs2Player.getRealSkillLevel(Skill.ATTACK);

        if (Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("pickaxe"))) {
            return true;
        }

        for (int i = 0; i < PICKAXES.length; i++) {
            String name = PICKAXES[i];
            if (Rs2Inventory.hasItem(name)) {
                if (attackLevel >= ATTACK_REQ[i] && miningLevel >= MINING_REQ[i]) {
                    Rs2Inventory.interact(name, "Wield");
                }
                return true;
            }
        }

        if (!Rs2Bank.isOpen()) {
            Microbot.status = "Walking to bank";
            Rs2Bank.walkToBankAndUseBank();
            return false;
        }

        for (int i = 0; i < PICKAXES.length; i++) {
            if (miningLevel >= MINING_REQ[i] && Rs2Bank.hasItem(PICKAXES[i])) {
                Microbot.status = "Withdrawing pickaxe";
                Rs2Bank.withdrawItem(true, PICKAXES[i]);
                if (attackLevel >= ATTACK_REQ[i]) {
                    Rs2Inventory.interact(PICKAXES[i], "Wield");
                }
                break;
            }
        }

        Rs2Bank.closeBank();

        return Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("pickaxe")) || Rs2Inventory.hasItem("pickaxe");
    }

    private void startPlugin(Class<? extends Plugin> clazz) { }

    private void stopPlugin(Class<? extends Plugin> clazz) { }
}
