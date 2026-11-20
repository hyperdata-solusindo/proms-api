package com.hyperdata.proms.module.work.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.module.work.board.constant.TemplateColumnCategory;
import com.hyperdata.proms.module.work.board.entity.TemplateColumn;
import com.hyperdata.proms.module.work.board.entity.WorkflowTemplate;
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
class WorkflowTemplateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    @Test
    @DisplayName("Harus dapat menyimpan dan mengambil workflow template beserta kolomnya")
    void shouldSaveAndRetrieveTemplateWithColumns() {
        UUID tenantId = UUID.randomUUID();
        WorkflowTemplate template = new WorkflowTemplate();
        template.setTenantId(tenantId);
        template.setName("Agile Kanban");

        TemplateColumn col1 = new TemplateColumn();
        col1.setTemplate(template);
        col1.setName("To do");
        col1.setCategory(TemplateColumnCategory.NOT_STARTED);
        col1.setPosition("a1");

        TemplateColumn col2 = new TemplateColumn();
        col2.setTemplate(template);
        col2.setName("Done");
        col2.setCategory(TemplateColumnCategory.DONE);
        col2.setPosition("a2");

        template.getColumns().add(col1);
        template.getColumns().add(col2);

        WorkflowTemplate saved = templateRepository.saveAndFlush(template);
        entityManager.clear();

        var found = templateRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Agile Kanban");
        assertThat(found.get().getColumns()).hasSize(2);

        var allAvailable = templateRepository.findAllAvailable(tenantId);
        assertThat(allAvailable).isNotEmpty();
    }
}
