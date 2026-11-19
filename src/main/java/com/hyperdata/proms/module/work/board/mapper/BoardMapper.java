package com.hyperdata.proms.module.work.board.mapper;

import com.hyperdata.proms.module.work.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.work.board.dto.BoardSummaryResponse;
import com.hyperdata.proms.module.work.board.dto.SavedViewResponse;
import com.hyperdata.proms.module.work.board.entity.Board;
import com.hyperdata.proms.module.work.board.entity.BoardColumn;
import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import com.hyperdata.proms.module.work.board.entity.Workstream;
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

    @Mapping(target = "taskCount", source = "taskCount")
    BoardDetailResponse.BoardColumnResponse toColumnResponse(BoardColumn column, Integer taskCount);

    default BoardDetailResponse.BoardColumnResponse toColumnResponse(BoardColumn column) {
        return toColumnResponse(column, 0);
    }

    BoardDetailResponse.WorkstreamResponse toWorkstreamResponse(Workstream workstream);

    @Mapping(target = "type", source = "viewType")
    @Mapping(target = "isDefault", source = "default")
    @Mapping(
        target = "version",
        expression = "java(\"\\\"\" + (view.getUpdatedAt() != null ? view.getUpdatedAt().toEpochMilli() : 0) + \"\\\"\")")
    SavedViewResponse toSavedViewResponse(BoardSavedView view);
}
