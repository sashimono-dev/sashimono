package dev.sashimono.builder.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Simple multi threaded task executor
 */
public class TaskRunner implements AutoCloseable {

    volatile ExecutorService executorService;

    volatile boolean started = false;

    private final List<Task<?>> tasks = new ArrayList<>();

    private final CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

    private volatile CountDownLatch latch;

    public <T> Task<T> newTask(Class<T> type, Function<TaskMap, T> task) {
        Task<T> ret = new Task<>(this, type, task);
        tasks.add(ret);
        return ret;
    }

    public void run() {
        if (started) {
            throw new IllegalStateException("already started");
        }
        started = true;
        latch = new CountDownLatch(tasks.size());
        for (var i : tasks) {
            i.maybeSchedule();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        executorService.execute(new Runnable() {
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

    @Override
    public void close() throws Exception {
        executorService.shutdown();
        executorService = null;
    }
}
