package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;

public class WoodcuttingTrainer implements SkillTrainer {
    private static final String[] AXES = {
            "Rune axe",
            "Adamant axe",
            "Mithril axe",
            "Black axe",
            "Steel axe",
            "Iron axe",
            "Bronze axe"
    };

    private static final int[] WOODCUTTING_REQ = {41, 31, 21, 11, 6, 1, 1};
    private static final int[] AXE_ATTACK_REQ = {40, 30, 20, 10, 5, 1, 1};

    private boolean waitingForAnim = false;
    private long animWaitStart = 0L;

    private boolean ensureAxe() {
        int wcLevel = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
        int attackLevel = Rs2Player.getRealSkillLevel(Skill.ATTACK);

        if (Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("axe"))) {
            return true;
        }

        for (int i = 0; i < AXES.length; i++) {
            String name = AXES[i];
            if (Rs2Inventory.hasItem(name)) {
                if (attackLevel >= AXE_ATTACK_REQ[i] && wcLevel >= WOODCUTTING_REQ[i]) {
                    Rs2Inventory.interact(name, "Wield");
                }
                return true;
            }
        }

        if (!Rs2Bank.isOpen()) {
            Microbot.status = "Walking to Bank";
            Rs2Bank.walkToBankAndUseBank();
            return false;
        }

        for (int i = 0; i < AXES.length; i++) {
            if (wcLevel >= WOODCUTTING_REQ[i] && Rs2Bank.hasItem(AXES[i])) {
                Microbot.status = "Withdrawing axe";
                Rs2Bank.withdrawItem(true, AXES[i]);
                if (attackLevel >= AXE_ATTACK_REQ[i]) {
                    Rs2Inventory.interact(AXES[i], "Wield");
                }
                break;
            }
        }

        Rs2Bank.closeBank();

        return Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("axe")) ||
                Rs2Inventory.contains(item -> item.getName().toLowerCase().contains("axe"));
    }

    public void trainLowLevelWoodcutting() {
        if (!ensureAxe()) {
            Microbot.status = "Getting axe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking logs";
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll();
                Rs2Bank.closeBank();
            }
            return;
        }

        WorldPoint trees = new WorldPoint(3162, 3454, 0);
        if (Rs2Player.getWorldLocation().distanceTo(trees) > 5) {
            Microbot.status = "Walking to trees";
            Rs2Walker.walkTo(trees);
            return;
        }

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Woodcutting";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Woodcutting";
            return;
        }

        GameObject tree = Rs2GameObject.findReachableObject("Tree", true, 10, trees);
        if (tree != null && Rs2GameObject.interact(tree)) {
            Microbot.status = "Woodcutting";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.WOODCUTTING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    public void trainOakWoodcutting() {
        if (!ensureAxe()) {
            Microbot.status = "Getting axe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking logs";
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll();
                Rs2Bank.closeBank();
            }
            return;
        }

        WorldPoint oaks = new WorldPoint(3192, 3461, 0);
        if (Rs2Player.getWorldLocation().distanceTo(oaks) > 5) {
            Microbot.status = "Walking to trees";
            Rs2Walker.walkTo(oaks);
            return;
        }

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Woodcutting";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Woodcutting";
            return;
        }

        GameObject tree = Rs2GameObject.findReachableObject("Oak", true, 10, oaks);
        if (tree != null && Rs2GameObject.interact(tree)) {
            Microbot.status = "Woodcutting";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.WOODCUTTING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    public void trainWillowWoodcutting() {
        if (!ensureAxe()) {
            Microbot.status = "Getting axe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking logs";
            if (Rs2DepositBox.walkToAndUseDepositBox()) {
                Rs2DepositBox.depositAll("Willow logs");
                Rs2DepositBox.closeDepositBox();
            }
            return;
        }

        WorldPoint willows = new WorldPoint(3060, 3254, 0);
        if (Rs2Player.getWorldLocation().distanceTo(willows) > 5) {
            Microbot.status = "Walking to trees";
            Rs2Walker.walkTo(willows);
            return;
        }

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Woodcutting";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Woodcutting";
            return;
        }

        GameObject tree = Rs2GameObject.findReachableObject("Willow", true, 10, willows);
        if (tree != null && Rs2GameObject.interact(tree)) {
            Microbot.status = "Woodcutting";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.WOODCUTTING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    @Override
    public void train() {
        int wcLevel = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
        if (wcLevel < 15) {
            trainLowLevelWoodcutting();
        } else if (wcLevel < 30) {
            trainOakWoodcutting();
        } else if (wcLevel < 60) {
            trainWillowWoodcutting();
        }
    }
}