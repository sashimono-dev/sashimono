package dev.sashimono.builder.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskMap {

    private final Map<Class<?>, List<Object>> results;

    public TaskMap(Collection<Task<?>> tasks, Map<Class<?>, List<ResultMapper>> mappers) {
        results = new HashMap<>();
        for (var i : tasks) {
            results.computeIfAbsent(i.type(), s -> new ArrayList<>()).add(i.value());
            if (mappers.containsKey(i.type())) {
                for (var m : mappers.get(i.type())) {
                    var res = m.map(i.value());
                    results.computeIfAbsent(res.getClass(), s -> new ArrayList<>()).add(res);
                }
            }
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
