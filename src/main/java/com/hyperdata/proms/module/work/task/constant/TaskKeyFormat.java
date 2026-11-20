package com.hyperdata.proms.module.work.task.constant;

import java.util.regex.Pattern;

public final class TaskKeyFormat {

    private TaskKeyFormat() {
    }

    public static final String SEPARATOR = "-";
    public static final Pattern PATTERN = Pattern.compile("^([A-Z]{2,5})-(\\d+)$");
}
