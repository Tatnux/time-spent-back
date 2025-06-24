package com.collet.timetracker.service.activity.reference;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class ReferenceCollection<T> {

    Map<Reference<T>, List<Consumer<T>>> map = new HashMap<>();

    public void add(Reference<T> reference, Consumer<T> consumer) {
        this.map.computeIfAbsent(reference, _ -> new ArrayList<>()).add(consumer);
    }
}
