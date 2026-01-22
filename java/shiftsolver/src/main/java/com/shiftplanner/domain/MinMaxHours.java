package com.shiftplanner.domain;

import java.time.Duration;

public class MinMaxHours {

    private Employee employee;
    private Duration max;
    private Duration min;

    public MinMaxHours(Employee employee, long min, long max) {

        this.employee = employee;
        this.max = Duration.ofHours(max);
        this.min = Duration.ofHours(min);


    }

    public Duration getMax() {
        return max;
    }
    public void setMax(Duration max) {
        this.max = max;
    }
    public Duration getMin() {
        return min;
    }
    public void setMin(Duration min) {
        this.min = min;
    }
    public Employee getEmployee() {
        return employee;
    }
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Boolean between(Duration d){
        return min.compareTo(d) <= 0 && max.compareTo(d) >= 0;
    }
    public Duration getDifference(Duration d){
        if(min.compareTo(d) > 0){
            return min.minus(d);
        }
        else if(max.compareTo(d) < 0)
            return d.minus(max);
        else
            return Duration.ZERO;
    }
}
