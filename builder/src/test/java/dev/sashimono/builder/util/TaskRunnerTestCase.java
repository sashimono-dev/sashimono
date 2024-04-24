package dev.sashimono.builder.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class TaskRunnerTestCase {
    @Test
    public void testSingleTask() {
        TaskRunner t = new TaskRunner();
        Task<String> task = t.newTask(String.class, (m) -> "Hello");
        t.run();
        Assertions.assertEquals("Hello", task.value());

    }

    @Test
    public void testChainedTasks() {
        TaskRunner t = new TaskRunner();
        Task<String> hello = t.newTask(String.class, (m) -> "Hello");
        Task<String> task = t.newTask(String.class, (m) -> String.join(" ", m.results(String.class)) + " Task");
        task.addDependency(hello);
        Task<String> world = t.newTask(String.class, (m) -> String.join(" ", m.results(String.class)) + " World");
        world.addDependency(task);
        t.run();
        Assertions.assertEquals("Hello Task World", world.value());
    }

    @RepeatedTest(100)
    public void testStableDependencyOrder() {
        TaskRunner t = new TaskRunner();
        Task<String> hello = t.newTask(String.class, (m) -> "Hello");
        Task<String> task = t.newTask(String.class, (m) -> "Stable");
        Task<String> world = t.newTask(String.class, (m) -> String.join(" ", m.results(String.class)) + " World");
        world.addDependency(hello);
        world.addDependency(task);
        t.run();
        Assertions.assertEquals("Hello Stable World", world.value());
    }

}
