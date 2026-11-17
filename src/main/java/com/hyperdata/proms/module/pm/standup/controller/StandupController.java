package com.hyperdata.proms.module.pm.standup.controller;

import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.pm.standup.dto.CreateStandupRequest;
import com.hyperdata.proms.module.pm.standup.dto.StandupDetailResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupListResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupTodayResponse;
import com.hyperdata.proms.module.pm.standup.dto.StandupUpdateResponse;
import com.hyperdata.proms.module.pm.standup.dto.UpdateStandupRequest;
import com.hyperdata.proms.module.pm.standup.service.StandupService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StandupController implements StandupApi {

    private final StandupService standupService;

    @Override
    public RestResponse<StandupListResponse> getStandups() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(standupService.getStandups(currentUser), "Berhasil");
    }

    @Override
    public RestResponse<StandupTodayResponse> getToday() {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(standupService.getToday(currentUser), "Berhasil");
    }

    @Override
    public RestResponse<List<StandupDetailResponse>> getMyStandups(LocalDate from, LocalDate to) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(standupService.getMyStandups(currentUser, from, to), "Berhasil");
    }

    @Override
    public RestResponse<StandupDetailResponse> createStandup(CreateStandupRequest request) {
        CreateStandupRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.created(standupService.createStandup(currentUser, request), "Standup terkirim");
    }

    @Override
    public RestResponse<StandupUpdateResponse> updateStandup(
        UUID standupId,
        String ifMatch,
        UpdateStandupRequest request) {
        UpdateStandupRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.ok(
            standupService.updateStandup(currentUser, standupId, ifMatch, request),
            "Standup diperbarui");
    }
}
