package com.hyperdata.proms.module.pm.board.mapper;

import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.pm.board.entity.Board;
import com.hyperdata.proms.module.pm.board.entity.BoardColumn;
import com.hyperdata.proms.module.pm.board.entity.Workstream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BoardMapper {

    @Mapping(target = "id", source = "board.id")
    @Mapping(target = "version", source = "board.updatedAt")
    @Mapping(target = "pm", source = "pm")
    @Mapping(target = "memberCount", source = "memberCount")
    @Mapping(target = "myRole", source = "myRole")
    BoardSummaryResponse toSummaryResponse(Board board, BoardSummaryResponse.PmSummaryResponse pm, int memberCount,
        String myRole);

    BoardDetailResponse.BoardColumnResponse toColumnResponse(BoardColumn column);

    BoardDetailResponse.WorkstreamResponse toWorkstreamResponse(Workstream workstream);
}
