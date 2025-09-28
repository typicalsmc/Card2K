package org.Card2K.scheduler;

import org.Card2K.NapThePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class SpigotSchedulerAdapter implements SchedulerAdapter {
    private final NapThePlugin plugin;
    private BukkitTask repeatingTask;

    public SpigotSchedulerAdapter(NapThePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void startRepeatingTask(Runnable task, long delayTicks) {
        if (task == null || delayTicks < 1) return;
        repeatingTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, delayTicks);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        if (task == null || delayTicks < 1) return;
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runTask(NapThePlugin plugin, Runnable task) {
        if (task == null) return;
        Bukkit.getScheduler().runTask(this.plugin, task);
    }

    @Override
    public void cancelAll() {
        if (repeatingTask != null) {
            repeatingTask.cancel();
            repeatingTask = null;
        }
    }
}
