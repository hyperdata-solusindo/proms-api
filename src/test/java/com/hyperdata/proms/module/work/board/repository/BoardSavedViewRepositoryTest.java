package com.hyperdata.proms.module.work.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.BoardViewType;
import com.hyperdata.proms.module.work.board.entity.BoardSavedView;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BoardSavedViewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BoardSavedViewRepository savedViewRepository;

    @Test
    @DisplayName("Harus dapat menyimpan dan membaca BoardSavedView dengan konfigurasi JSONB")
    void shouldSaveAndRetrieveSavedViewWithJsonb() {
        UUID tenantId = UUID.randomUUID();
        UUID boardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BoardSavedView view = BoardSavedView.builder()
            .tenantId(tenantId)
            .boardId(boardId)
            .userId(userId)
            .name("High Priority Hardware")
            .viewType(BoardViewType.LIST)
            .filters(Map.of("priority", "high", "overdue", true))
            .sortConfig(Map.of("dueDate", "asc"))
            .isDefault(true)
            .position(1)
            .build();

        savedViewRepository.saveAndFlush(view);
        entityManager.clear();

        var list = savedViewRepository.findByTenantIdAndBoardIdAndUserId(tenantId, boardId, userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("High Priority Hardware");
        assertThat(list.get(0).getFilters()).containsEntry("priority", "high");
    }
}
