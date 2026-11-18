package com.hyperdata.proms.module.pm.focus.controller;

import com.hyperdata.proms.common.exception.ValidationException;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.common.util.Messages;
import com.hyperdata.proms.module.pm.focus.dto.CreateFocusSessionRequest;
import com.hyperdata.proms.module.pm.focus.dto.FocusSessionListResult;
import com.hyperdata.proms.module.pm.focus.dto.FocusSessionResponse;
import com.hyperdata.proms.module.pm.focus.service.FocusService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FocusController implements FocusApi {

    private final FocusService focusService;

    @Override
    public RestResponse<List<FocusSessionResponse>> listSessions(LocalDate from, LocalDate to) {
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        FocusSessionListResult result = focusService.list(currentUser, from, to);
        return RestResponse.ok(
            result.sessions(),
            Map.of("summary", result.summary()),
            Messages.get("focus.list.retrieved"));
    }

    @Override
    public RestResponse<FocusSessionResponse> createSession(CreateFocusSessionRequest request) {
        CreateFocusSessionRequest.validator.validate(request).throwIfInvalid(ValidationException::new);
        CurrentUser currentUser = SecurityContextHelper.getRequiredCurrentUser();
        return RestResponse.created(
            focusService.create(currentUser, request),
            Messages.get("focus.session.created"));
    }
}
