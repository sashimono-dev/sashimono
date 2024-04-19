package dev.sashimono.builder.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Simple multi threaded task executor
 */
public class TaskRunner {

    volatile ExecutorService executorService;
    volatile ExecutorService backgroundExecutor;

    volatile boolean started = false;

    private final List<Task<?>> tasks = new ArrayList<>();

    private final CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

    private volatile CountDownLatch latch;

    public <T> Task<T> newTask(Class<T> type, Function<TaskMap, T> task) {
        Task<T> ret = new Task<>(this, type, task, false);
        tasks.add(ret);
        return ret;
    }

    public <T> Task<T> newBackgroundTask(Class<T> type, Function<TaskMap, T> task) {
        Task<T> ret = new Task<>(this, type, task, true);
        tasks.add(ret);
        return ret;
    }

    public void run() {
        if (started) {
            throw new IllegalStateException("already started");
        }
        started = true;
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        backgroundExecutor = Executors.newFixedThreadPool(8); //TODO: configurable
        latch = new CountDownLatch(tasks.size());
        try {
            for (var i : tasks) {
                i.start();
            }
            for (var i : tasks) {
                i.maybeSchedule();
            }
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
            backgroundExecutor.shutdown();
        }
        if (!errors.isEmpty()) {
            RuntimeException failure = new RuntimeException(errors.get(0));
            for (var i = 1; i < errors.size(); ++i) {
                failure.addSuppressed(errors.get(i));
            }
            throw failure;
        }
    }

    void schedule(Task<?> task) {
        ExecutorService ex = task.background() ? backgroundExecutor : executorService;
        ex.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } finally {
                    if (task.state() == Task.State.ERROR) {
                        errors.add(task.error());
                        latch.countDown();
                        task.forEachDependent((t) -> {
                            latch.countDown();
                            t.skip();
                        });
                    } else if (task.state() == Task.State.COMPLETE) {
                        latch.countDown();
                    } else {
                        errors.add(new Error("Implementation error, task is not in a final state"));
                        while (latch.getCount() > 0) {
                            latch.countDown();
                        }
                    }
                }
            }
        });
    }

}
