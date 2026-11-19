package com.hyperdata.proms.module.work.standup.mapper;

import com.hyperdata.proms.module.work.standup.dto.CreateStandupRequest;
import com.hyperdata.proms.module.work.standup.dto.StandupDetailResponse;
import com.hyperdata.proms.module.work.standup.dto.StandupTodayResponse;
import com.hyperdata.proms.module.work.standup.dto.StandupUpdateResponse;
import com.hyperdata.proms.module.work.standup.dto.UpdateStandupRequest;
import com.hyperdata.proms.module.work.standup.entity.StandupPolicies;
import com.hyperdata.proms.module.work.standup.entity.StandupReports;
import com.hyperdata.proms.module.work.standup.facade.dto.StandupPolicyDto;
import com.hyperdata.proms.module.work.standup.facade.dto.StandupReportSummaryDto;

import java.time.LocalDate;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StandupMapper {

    @Mapping(target = "blockers", source = "blockers", qualifiedByName = "blankToNull")
    StandupReports toEntity(CreateStandupRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "blockers", source = "blockers", qualifiedByName = "blankToNull")
    void updateEntity(UpdateStandupRequest request, @MappingTarget
    StandupReports report);

    @Mapping(target = "isLate", source = "late")
    @Mapping(target = "version", source = "updatedAt")
    StandupDetailResponse toDetailResponse(StandupReports report);

    @Mapping(target = "isLate", source = "late")
    @Mapping(target = "version", source = "updatedAt")
    StandupUpdateResponse toUpdateResponse(StandupReports report);

    @Mapping(target = "reportDate", source = "reportDate")
    @Mapping(target = "required", source = "required")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "report", source = "report")
    @Mapping(target = "deadlineTime", source = "policy.deadlineTime", dateFormat = "HH:mm")
    @Mapping(target = "blockingBanner", source = "policy.blockingBanner")
    StandupTodayResponse toTodayResponse(LocalDate reportDate, boolean required, String reason,
        StandupReports report, StandupPolicies policy);

    @Mapping(target = "isLate", source = "late")
    StandupReportSummaryDto toReportSummaryDto(StandupReports report);

    StandupPolicyDto toPolicyDto(StandupPolicies policy);

    @Named("blankToNull")
    default String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
