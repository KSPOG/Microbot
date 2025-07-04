package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import java.util.Arrays;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;

public class MiningTrainer implements SkillTrainer {
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

    private boolean waitingForAnim = false;
    private long animWaitStart = 0L;

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
            Microbot.status = "Walking to Bank";
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

        return Rs2Equipment.isWearing(item -> item.getName().toLowerCase().contains("pickaxe")) ||
                Rs2Inventory.contains(item -> item.getName().toLowerCase().contains("pickaxe"));
    }

    private void depositUncutGems() {
        Rs2Bank.depositAll("Uncut diamond");
        Rs2Bank.depositAll("Uncut ruby");
        Rs2Bank.depositAll("Uncut emerald");
        Rs2Bank.depositAll("Uncut sapphire");
    }

    public void trainLowLevelMining() {
        if (!ensurePickaxe()) {
            Microbot.status = "Getting pickaxe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking ore";
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll("tin ore");
                Rs2Bank.depositAll("copper ore");
                depositUncutGems();
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
                Microbot.status = "Mining";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
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

        GameObject rock = Rs2GameObject.findReachableObject(rockName, true, 10,
                Rs2Player.getWorldLocation());
        if (rock != null && Rs2GameObject.interact(rock)) {
            Microbot.status = "Mining";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    @Override
    public void train() {
        int miningLevel = Rs2Player.getRealSkillLevel(Skill.MINING);
        if (miningLevel >= 30) {
            trainCoalMining();
        } else if (miningLevel >= 15) {
            trainIronMining();
        } else {
            trainLowLevelMining();
        }
    }

    public void trainIronMining() {
        if (!ensurePickaxe()) {
            Microbot.status = "Getting pickaxe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking ore";
            if (Rs2DepositBox.walkToAndUseDepositBox()) {
                Rs2DepositBox.depositAllExcept(Arrays.asList("pickaxe"), false);
                Rs2DepositBox.closeDepositBox();
            }
            return;
        }

        WorldPoint mine = new WorldPoint(2970, 3239, 0);
        if (Rs2Player.getWorldLocation().distanceTo(mine) > 5) {
            Microbot.status = "Walking to mine";
            Rs2Walker.walkTo(mine);
            return;
        }

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Mining";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Mining";
            return;
        }

        GameObject rock = Rs2GameObject.findReachableObject("Iron rocks", true, 10,
                Rs2Player.getWorldLocation());
        if (rock != null && Rs2GameObject.interact(rock)) {
            Microbot.status = "Mining";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    public void trainCoalMining() {
        if (!ensurePickaxe()) {
            Microbot.status = "Getting pickaxe";
            return;
        }

        if (Rs2Inventory.isFull()) {
            Microbot.status = "Banking ore";
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll("coal");
                Rs2Bank.depositAll("iron ore");
                Rs2Bank.depositAll("copper ore");
                Rs2Bank.depositAll("tin ore");
                depositUncutGems();
                Rs2Bank.closeBank();
            }
            return;
        }

        WorldPoint mine = new WorldPoint(3083, 3422, 0);
        if (Rs2Player.getWorldLocation().distanceTo(mine) > 5) {
            Microbot.status = "Walking to mine";
            Rs2Walker.walkTo(mine);
            return;
        }

        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Mining";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }

        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Mining";
            return;
        }

        GameObject rock = Rs2GameObject.findReachableObject("Coal rocks", true, 10,
                Rs2Player.getWorldLocation());
        if (rock != null && Rs2GameObject.interact(rock)) {
            Microbot.status = "Mining";
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }
}