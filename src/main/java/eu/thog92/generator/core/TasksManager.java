package eu.thog92.generator.core;

import eu.thog92.generator.api.tasks.ITaskManager;
import eu.thog92.generator.api.tasks.ScheduledTask;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TasksManager implements ITaskManager
{

    private final HashMap<ScheduledTask, ScheduledFuture<?>> activeTasks = new HashMap<>();
    private ScheduledExecutorService scheduler = Executors
            .newScheduledThreadPool(4);

    public void scheduleTask(ScheduledTask task)
    {
        System.out.println("Scheduling " + task.getName() + "...");
        this.activeTasks.put(task, scheduler.scheduleAtFixedRate(task, 0,
                task.getDelay(), TimeUnit.SECONDS));
    }

    public void onFinishTask(ScheduledTask task)
    {
        if (task.isCancelled())
        {
            if (this.activeTasks.get(task) != null)
            {
                this.activeTasks.remove(task).cancel(true);
            }
        }
    }

    public ScheduledExecutorService getScheduler()
    {
        return scheduler;
    }
}
