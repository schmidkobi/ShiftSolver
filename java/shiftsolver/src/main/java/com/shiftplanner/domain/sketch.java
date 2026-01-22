package com.shiftplanner.domain;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class sketch {
    public static void main(String[] args) {
        MinMaxHours range = new MinMaxHours(new Employee(),10, 20);
        System.out.println(range.getDifference(Duration.ofHours(5)));
        System.out.println(range.getDifference(Duration.ofHours(15)));
        System.out.println(range.getDifference(Duration.ofHours(30)));
    }
}
