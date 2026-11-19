package com.hyperdata.proms.module.work.board.repository.spec;

import com.hyperdata.proms.module.work.board.constant.BoardMemberRole;
import com.hyperdata.proms.module.work.board.constant.BoardStatus;
import com.hyperdata.proms.module.work.board.constant.BoardTab;
import com.hyperdata.proms.module.work.board.entity.Board;
import com.hyperdata.proms.module.work.board.entity.BoardMember;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BoardSpecs {

    private BoardSpecs() {
    }

    public static Specification<Board> withFilters(UUID tenantId, UUID userId, String tab, String q) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (tenantId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("tenantId"), tenantId));
            }
            predicates = cb.and(predicates, cb.isNull(root.get("deletedAt")));

            // Filter tab
            Subquery<UUID> memberSubquery = query.subquery(UUID.class);
            var memberRoot = memberSubquery.from(BoardMember.class);
            memberSubquery.select(memberRoot.get("boardId"));

            if (BoardTab.ARCHIVED.equalsIgnoreCase(tab)) {
                predicates = cb.and(predicates, cb.isNotNull(root.get("archivedAt")));
                memberSubquery.where(cb.equal(memberRoot.get("userId"), userId));
                predicates = cb.and(predicates, root.get("id").in(memberSubquery));
            } else if (BoardTab.SHARED.equalsIgnoreCase(tab)) {
                predicates = cb.and(predicates, cb.isNull(root.get("archivedAt")));
                predicates = cb.and(predicates, cb.equal(root.get("status"), BoardStatus.ACTIVE));
                memberSubquery.where(
                    cb.equal(memberRoot.get("userId"), userId),
                    cb.isFalse(memberRoot.get("isOwner")),
                    cb.notEqual(memberRoot.get("role"), BoardMemberRole.ADMIN));
                predicates = cb.and(predicates, root.get("id").in(memberSubquery));
            } else { // default "mine"
                predicates = cb.and(predicates, cb.isNull(root.get("archivedAt")));
                predicates = cb.and(predicates, cb.equal(root.get("status"), BoardStatus.ACTIVE));
                memberSubquery.where(cb.equal(memberRoot.get("userId"), userId));
                predicates = cb.and(predicates, root.get("id").in(memberSubquery));
            }

            // Filter keyword q
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                var nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                var keyMatch = cb.like(cb.lower(root.get("key")), pattern);
                predicates = cb.and(predicates, cb.or(nameMatch, keyMatch));
            }

            return predicates;
        };
    }
}
