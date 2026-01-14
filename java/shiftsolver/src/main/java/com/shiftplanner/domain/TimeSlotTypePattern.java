package com.shiftplanner.domain;

import java.util.*;
import java.util.stream.Collectors;

public class TimeSlotTypePattern {
    private final List<String> typePattern; // immutable list

    public TimeSlotTypePattern(String... types) {
        if (types == null) throw new NullPointerException("types == null");
        // Defensive copy and ensure no null items (optional)
        for (String s : types) {
            if (s == null) throw new NullPointerException("element in types is null");
        }
        this.typePattern = Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(types, types.length)));
    }

    public TimeSlotTypePattern(List<String> elements) {
        if (elements == null) throw new NullPointerException("elements == null");
        for (String s : elements) {
            if (s == null) throw new NullPointerException("element in elements is null");
        }
        this.typePattern = Collections.unmodifiableList(List.copyOf(elements));
    }

    public List<String> getTypePattern() {
        return typePattern;
    }

    public int size() {
        return typePattern.size();
    }

    public String get(int index) {
        return typePattern.get(index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlotTypePattern)) return false;
        TimeSlotTypePattern that = (TimeSlotTypePattern) o;
        return typePattern.equals(that.typePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typePattern);
    }

    @Override
    public String toString() {
        return "Combination" + typePattern.toString();
    }
}

