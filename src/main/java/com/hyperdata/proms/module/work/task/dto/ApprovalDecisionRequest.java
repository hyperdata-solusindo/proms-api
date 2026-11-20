package com.hyperdata.proms.module.work.task.dto;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import am.ik.yavi.core.ViolationMessage;
import com.hyperdata.proms.module.work.task.constant.ApprovalDecision;
import com.hyperdata.proms.module.work.task.constant.TaskLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Keputusan approval dari Inbox")
public record ApprovalDecisionRequest(
    @Schema(
        description = "Keputusan",
        example = ApprovalDecision.REJECTED,
        allowableValues = {
            ApprovalDecision.APPROVED, ApprovalDecision.REJECTED},
        requiredMode = Schema.RequiredMode.REQUIRED)
    String decision,

    @Schema(description = "Alasan (wajib bila rejected)", example = "Nota belum dilampirkan")
    String reason){

    private static final List<String> DECISIONS = List.of(ApprovalDecision.APPROVED, ApprovalDecision.REJECTED);

    public static final Validator<ApprovalDecisionRequest> validator = ValidatorBuilder.<ApprovalDecisionRequest>of()
        .constraint(ApprovalDecisionRequest::decision, "decision", c -> c
            .predicate(
                DECISIONS::contains,
                ViolationMessage.of(
                    "BR-NT-INVALID-DECISION",
                    "Keputusan tidak valid. Pilihan: " + String.join(", ", DECISIONS))))
        .constraint(ApprovalDecisionRequest::reason, "reason", c -> c
            .lessThanOrEqual(TaskLimits.DECISION_REASON_MAX_LENGTH)
            .message("Alasan maksimal " + TaskLimits.DECISION_REASON_MAX_LENGTH + " karakter"))
        .constraintOnTarget(
            r -> !ApprovalDecision.REJECTED.equals(r.decision()) || (r.reason() != null && !r.reason().isBlank()),
            "reason",
            "BR-NT-REASON-REQUIRED",
            "Alasan wajib diisi saat menolak")
        .build();
}
