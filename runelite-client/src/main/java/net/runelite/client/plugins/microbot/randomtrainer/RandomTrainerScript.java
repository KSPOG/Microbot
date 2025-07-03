package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.randomtrainer.MiningTrainer;
import net.runelite.client.plugins.microbot.randomtrainer.WoodcuttingTrainer;
import org.apache.commons.lang3.time.DurationFormatUtils;

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
    private boolean idleForBreak = false;

    private final MiningTrainer miningTrainer = new MiningTrainer();
    private final WoodcuttingTrainer woodcuttingTrainer = new WoodcuttingTrainer();

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

    public String getTimeRunning() {
        return DurationFormatUtils.formatDuration(getRunTime().toMillis(), "HH:mm:ss", true);
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
            if (Rs2Bank.walkToBankAndUseBank()) {
                Rs2Bank.depositAll();
                Rs2Bank.closeBank();
            }
            idleForBreak = true;
        }
        sleep(1000);
    }

    private void bankInventory() {
        Microbot.status = "Walking to Bank";
        if (!Rs2Bank.isOpen()) {
            if (!Rs2Bank.walkToBankAndUseBank()) {
                return;
            }
        }
        Rs2Bank.depositAll();
        Rs2Bank.closeBank();
    }

    private void selectNewTask() {
        bankInventory();

        SkillTask[] available = { SkillTask.MINING, SkillTask.WOODCUTTING };
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
                int miningLevel = Rs2Player.getRealSkillLevel(Skill.MINING);
                if (miningLevel >= 30) {
                    miningTrainer.trainCoalMining();
                } else if (miningLevel >= 15) {
                    miningTrainer.trainIronMining();
                } else {
                    miningTrainer.trainLowLevelMining();
                }
                break;
            case WOODCUTTING:
                Microbot.status = "Woodcutting";
                int wcLevel = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
                if (wcLevel < 15) {
                    woodcuttingTrainer.trainLowLevelWoodcutting();
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


    // Reserved for future plugin integrations
}