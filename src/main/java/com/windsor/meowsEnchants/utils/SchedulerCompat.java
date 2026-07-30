package com.windsor.meowsEnchants.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerCompat {

    private SchedulerCompat() {
    }

    public interface Task {
        void cancel();
    }

    public static Task run(Entity entity, Plugin plugin, Consumer<Task> task) {
        Task foliaTask = trySchedule(entity, plugin, task, "run", null, null);
        if (foliaTask != null) {
            return foliaTask;
        }

        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, () -> task.accept(() -> {
        }));
        return bukkitTask::cancel;
    }

    public static Task runDelayed(Entity entity, Plugin plugin, Consumer<Task> task, long delayTicks) {
        Task foliaTask = trySchedule(entity, plugin, task, "runDelayed", delayTicks, null);
        if (foliaTask != null) {
            return foliaTask;
        }

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, () -> task.accept(() -> {
        }), delayTicks);
        return bukkitTask::cancel;
    }

    public static Task runAtFixedRate(Entity entity, Plugin plugin, Consumer<Task> task,
                                      long initialDelayTicks, long periodTicks) {
        Task foliaTask = trySchedule(entity, plugin, task, "runAtFixedRate", initialDelayTicks, periodTicks);
        if (foliaTask != null) {
            return foliaTask;
        }

        final BukkitTask[] handle = new BukkitTask[1];
        handle[0] = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> task.accept(() -> {
                    if (handle[0] != null) {
                        handle[0].cancel();
                    }
                }),
                initialDelayTicks,
                periodTicks);
        return () -> handle[0].cancel();
    }

    private static Task trySchedule(Entity entity, Plugin plugin, Consumer<Task> task,
                                    String methodName, Long delayOrInitialDelay, Long period) {
        try {
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(entity);
            Consumer<Object> foliaConsumer = scheduledTask -> task.accept(() -> cancelScheduledTask(scheduledTask));

            Object scheduledTask;
            if ("run".equals(methodName)) {
                Method method = findMethod(scheduler.getClass(), methodName, 3);
                scheduledTask = method.invoke(scheduler, plugin, foliaConsumer, null);
            } else if ("runDelayed".equals(methodName)) {
                Method method = findMethod(scheduler.getClass(), methodName, 4);
                scheduledTask = method.invoke(scheduler, plugin, foliaConsumer, null, delayOrInitialDelay);
            } else {
                Method method = findMethod(scheduler.getClass(), methodName, 5);
                scheduledTask = method.invoke(scheduler, plugin, foliaConsumer, null, delayOrInitialDelay, period);
            }

            if (scheduledTask == null) {
                return null;
            }
            return () -> cancelScheduledTask(scheduledTask);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void cancelScheduledTask(Object scheduledTask) {
        try {
            Method cancel = scheduledTask.getClass().getMethod("cancel");
            cancel.invoke(scheduledTask);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }
}
