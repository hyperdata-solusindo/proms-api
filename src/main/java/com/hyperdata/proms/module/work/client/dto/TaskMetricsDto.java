package com.hyperdata.proms.module.work.client.dto;

public record TaskMetricsDto(
    long todo,
    long doing,
    long blocked,
    long done,
    long total,
    long active,
    int progressPct) {

    public static TaskMetricsDto empty() {
        return new TaskMetricsDto(0, 0, 0, 0, 0, 0, 0);
    }
}
