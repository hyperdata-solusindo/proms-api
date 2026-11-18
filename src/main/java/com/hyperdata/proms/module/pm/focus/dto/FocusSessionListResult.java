package com.hyperdata.proms.module.pm.focus.dto;

import java.util.List;

public record FocusSessionListResult(
    List<FocusSessionResponse> sessions,
    FocusSummaryResponse summary) {

}
