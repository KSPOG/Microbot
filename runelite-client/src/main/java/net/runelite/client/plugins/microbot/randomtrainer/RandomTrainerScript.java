package net.runelite.client.plugins.microbot.randomtrainer;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.microbot.mining.AutoMiningPlugin;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

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
        currentTask = tasks[random.nextInt(tasks.length)];
        Microbot.status = "Selected " + currentTask.name();
    }

    private void executeCurrentTask() {
        Microbot.status = "Training " + currentTask.name();
        switch (currentTask) {
            case MINING:
                startPlugin(AutoMiningPlugin.class);
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
