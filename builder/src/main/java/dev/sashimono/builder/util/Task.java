package dev.sashimono.builder.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Task<T> {

    private static final AtomicReferenceFieldUpdater<Task, ObjectState> stateUpdater = AtomicReferenceFieldUpdater
            .newUpdater(Task.class, ObjectState.class, "state");

    private final TaskRunner taskRunner;
    private final Class<T> type;
    private final Function<TaskMap, T> task;
    private final Set<Task<?>> dependencies = new HashSet<>();
    private final Set<Task<?>> dependants = new HashSet<>();

    private final AtomicInteger outstanding = new AtomicInteger();

    private volatile ObjectState<T> state = new ObjectState<>(State.SETUP, null, null);

    public Task(TaskRunner taskRunner, Class<T> type, Function<TaskMap, T> task) {
        this.taskRunner = taskRunner;
        this.task = task;
        this.type = type;
    }

    void start() {
        requireStateSetup();
        state = new ObjectState<>(State.STARTED, null, null);
    }

    public void addDependency(Task<?> dependency) {
        requireStateSetup();
        dependency.requireStateSetup();
        if (taskRunner != dependency.taskRunner) {
            throw new IllegalArgumentException("tasks must have the same runner");
        }
        if (this.dependencies.contains(dependency)) {
            throw new IllegalStateException("already a dependency");
        }
        ensureNotDependant(dependency);
        this.dependencies.add(dependency);
        dependency.dependants.add(this);
    }

    private void ensureNotDependant(Task<?> dependency) {
        if (dependants.contains(dependency)) {
            throw new RuntimeException("circular dependency detected");
        }
        for (var i : dependants) {
            i.ensureNotDependant(dependency);
        }
    }

    void maybeSchedule() {
        do {
            var outstanding = this.outstanding.get();
            if (outstanding > 0) {
                return;
            }
            var state = stateUpdater.get(this);
            if (state.state == State.SETUP) {
                throw new IllegalStateException("cannot schedule a task before it has been started");
            }
            if (state.state != State.STARTED) {
                //already scheduled
                return;
            }
        } while (!stateUpdater.compareAndSet(this, state, new ObjectState(State.SCHEDULED, null, null)));
        taskRunner.schedule(this);
    }

    void run() {
        try {
            var result = task.apply(new TaskMap(dependencies));
            state = new ObjectState<>(State.COMPLETE, null, result);
            for (var i : dependants) {
                i.outstanding.decrementAndGet();
                i.maybeSchedule();
            }
        } catch (Throwable t) {
            state = new ObjectState<>(State.COMPLETE, t, null);
        }
    }

    public Class<T> type() {
        return type;
    }

    public T value() {
        if (state.state != State.COMPLETE) {
            throw new IllegalStateException("state must be COMPLETE");
        }
        return state.value;
    }

    private void requireStateSetup() {
        if (state.state != State.SETUP) {
            throw new IllegalStateException("state must be in SETUP");
        }
    }

    public State state() {
        return state.state;
    }

    public Throwable error() {
        return state.error;
    }

    void forEachDependent(Consumer<Task<?>> callback) {
        for (var i : dependants) {
            callback.accept(i);
            i.forEachDependent(callback);
        }
    }

    public void skip() {
        state = new ObjectState<>(State.SKIPPED, null, null);
    }

    public enum State {
        /**
         * The dependency tree is being setup, the dependency tree is still mutable
         */
        SETUP,
        /**
         * The task runner has been started, the dependency list is now immutable.
         * <p>
         * This object has not been scheduled yet, possibly due to dependencies not being complete,
         * although even objects with no dependencies will pass through this state.
         */
        STARTED,
        /**
         * The task has been scheduled with the TaskRunner
         */
        SCHEDULED,
        /**
         * Task failed
         */
        ERROR,
        /**
         * task complete successfully
         */
        COMPLETE,
        /**
         * task will not be run as a dependency failed
         */
        SKIPPED
    }

    record ObjectState<T>(State state, Throwable error, T value) {

    }
}
