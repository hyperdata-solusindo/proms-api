package com.hyperdata.proms.module.work.standup.constant;

import java.time.LocalTime;

public class StandupPolicyDefaults {

    private StandupPolicyDefaults() {
    }

    public static final LocalTime REMINDER_TIME = LocalTime.of(10, 0);
    public static final LocalTime DEADLINE_TIME = LocalTime.of(12, 0);
}
