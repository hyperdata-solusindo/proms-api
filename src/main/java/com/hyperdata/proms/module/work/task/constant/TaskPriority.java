package com.hyperdata.proms.module.work.task.constant;

import java.util.List;

public final class TaskPriority {

    private TaskPriority() {

    }

    public static final String LOW = "low";
    public static final String MEDIUM = "medium";
    public static final String HIGH = "high";
    public static final String URGENT = "urgent";

    public static final String DEFAULT = MEDIUM;
    public static final List<String> ALL = List.of(LOW, MEDIUM, HIGH, URGENT);
}
