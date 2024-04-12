package dev.sashimono.builder.util;

import java.util.*;

public class TaskMap {

    private final Map<Class<?>, List<Object>> results;

    public TaskMap(Collection<Task<?>> tasks) {
        results = new HashMap<>();
        for (var i : tasks) {
            results.computeIfAbsent(i.type(), s -> new ArrayList<>()).add(i.value());
        }
    }

    public <T> List<T> results(Class<T> type) {
        List<Object> ret = results.get(type);
        if (ret == null) {
            return List.of();
        }
        return (List<T>) ret;
    }
}
