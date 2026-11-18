package com.hyperdata.proms.module.pm.focus.mapper;

import com.hyperdata.proms.module.pm.focus.dto.FocusSessionResponse;
import com.hyperdata.proms.module.pm.focus.entity.FocusSession;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FocusMapper {

    @Mapping(target = "id", source = "session.id")
    @Mapping(target = "startedAt", source = "session.startedAt")
    @Mapping(target = "endedAt", source = "session.endedAt")
    @Mapping(target = "durationSec", source = "session.durationSec")
    @Mapping(target = "durationMin", expression = "java(session.getDurationSec() / 60)")
    @Mapping(target = "date", source = "date")
    @Mapping(target = "task", source = "task")
    FocusSessionResponse toResponse(FocusSession session, LocalDate date, FocusSessionResponse.TaskRef task);
}
