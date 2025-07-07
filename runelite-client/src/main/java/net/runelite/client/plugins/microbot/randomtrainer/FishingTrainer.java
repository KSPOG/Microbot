package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;

import java.util.Arrays;
import java.util.List;

public class FishingTrainer implements SkillTrainer {
    private final RandomTrainerScript script;
    private boolean waitingForAnim = false;
    private long animWaitStart = 0L;

    // use the exact item name so the script can withdraw it from the bank
    private static final List<String> FISHING_NET = Arrays.asList("small fishing net");
    private static final List<String> FLY_FISH_GEAR = Arrays.asList("Fly fishing rod", "Feather");

    public FishingTrainer(RandomTrainerScript script) {
        this.script = script;
    }

    private boolean ensureGear() {
        int lvl = Rs2Player.getRealSkillLevel(Skill.FISHING);
        if (lvl < 20) {
            if (Rs2Inventory.hasItem(FISHING_NET.get(0))) {
                return true;
            }
            if (!Rs2Bank.isOpen()) {
                Microbot.status = "Walking to Bank";
                Rs2Bank.walkToBankAndUseBank();
                return false;
            }
            if (Rs2Bank.withdrawItem(true, FISHING_NET.get(0))) {
                Rs2Bank.closeBank();
            }
            return Rs2Inventory.hasItem(FISHING_NET.get(0));
        } else {
            boolean hasRod = Rs2Inventory.hasItem("Fly fishing rod");
            boolean hasFeather = Rs2Inventory.hasItem("Feather");
            if (hasRod && hasFeather) return true;
            if (!Rs2Bank.isOpen()) {
                Microbot.status = "Walking to Bank";
                Rs2Bank.walkToBankAndUseBank();
                return false;
            }
            if (!hasRod) {
                Rs2Bank.withdrawItem(true, "Fly fishing rod");
            }
            if (!hasFeather) {
                Rs2Bank.withdrawAll(false, "Feather");
            }
            Rs2Bank.closeBank();
            return Rs2Inventory.hasItem("Fly fishing rod") && Rs2Inventory.hasItem("Feather");
        }
    }

    private void bankLowLevelFish() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank(new WorldPoint(3209, 3220, 2));
        }
        Rs2Bank.depositAll("Raw shrimps");
        Rs2Bank.depositAll("Raw anchovies");
        Rs2Bank.closeBank();
    }

    private void bankHighLevelFish() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
        }
        Rs2Bank.depositAllExcept("Fly fishing rod", "Feather");
        Rs2Bank.closeBank();
    }

    private boolean atLocation(WorldPoint wp) {
        return Rs2Player.getWorldLocation().distanceTo(wp) <= 5;
    }

    public void trainLowLevelFishing() {
        if (!ensureGear()) {
            Microbot.status = "Getting net";
            return;
        }
        if (Rs2Player.inventoryIsFull()) {
            Microbot.status = "Banking fish";
            bankLowLevelFish();
            return;
        }
        WorldPoint spot = new WorldPoint(3244, 3154, 0);
        if (!atLocation(spot)) {
            Microbot.status = "Walking to spot";
            Rs2Walker.walkTo(spot);
            return;
        }
        script.startSwitchTimerIfNeeded();
        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Fishing";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Fishing";
            return;
        }
        Rs2NpcModel npc = Rs2Npc.getNpcs("Fishing spot").findFirst().orElse(null);
        npc = Rs2Npc.validateInteractable(npc);
        if (npc != null && Rs2Npc.interact(npc, "Net")) {
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.FISHING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    public void trainFlyFishing() {
        if (!ensureGear()) {
            Microbot.status = "Getting rod";
            return;
        }
        if (Rs2Player.inventoryIsFull()) {
            Microbot.status = "Banking fish";
            bankHighLevelFish();
            return;
        }
        WorldPoint spot = new WorldPoint(3104, 3431, 0);
        if (!atLocation(spot)) {
            Microbot.status = "Walking to spot";
            Rs2Walker.walkTo(spot);
            return;
        }
        script.startSwitchTimerIfNeeded();
        if (waitingForAnim) {
            if (Rs2Player.isAnimating()) {
                waitingForAnim = false;
                Microbot.status = "Fishing";
            } else if (System.currentTimeMillis() - animWaitStart > 5000) {
                waitingForAnim = false;
                Microbot.status = "Idle";
            } else {
                return;
            }
        }
        if (Rs2Player.isAnimating() || Rs2Player.isMoving()) {
            Microbot.status = "Fishing";
            return;
        }
        Rs2NpcModel npc = Rs2Npc.getNpcs("Fishing spot").findFirst().orElse(null);
        npc = Rs2Npc.validateInteractable(npc);
        if (npc != null && Rs2Npc.interact(npc, "Lure")) {
            waitingForAnim = true;
            animWaitStart = System.currentTimeMillis();
            Rs2Player.waitForXpDrop(Skill.FISHING, true);
            Rs2Antiban.actionCooldown();
        } else {
            Microbot.status = "Idle";
        }
    }

    @Override
    public void train() {
        int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
        if (level < 20) {
            trainLowLevelFishing();
        } else {
            trainFlyFishing();
        }
    }
}
