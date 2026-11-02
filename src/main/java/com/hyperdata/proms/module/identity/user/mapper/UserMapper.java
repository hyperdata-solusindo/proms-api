package com.hyperdata.proms.module.identity.user.mapper;

import com.hyperdata.proms.module.identity.user.dto.UserProfileDto;
import com.hyperdata.proms.module.identity.user.entity.User;
import com.hyperdata.proms.module.identity.user.facade.dto.UserAuthDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserProfileDto toProfileDto(User user);

    @Mapping(target = "isActive", source = "active")
    UserAuthDto toAuthDto(User user);
}
