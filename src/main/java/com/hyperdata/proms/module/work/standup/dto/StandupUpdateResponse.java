package com.hyperdata.proms.module.work.standup.dto;

import java.util.UUID;

public record StandupUpdateResponse(
    UUID id,
    String plan,
    String done,
    String blockers,
    boolean isLate,
    String version) {

}
