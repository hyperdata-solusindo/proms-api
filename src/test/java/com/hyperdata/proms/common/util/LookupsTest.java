package com.hyperdata.proms.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Lookups Utility Tests")
class LookupsTest {

    record SampleItem(String id, String category, String name, BigDecimal price, String status) {
    }

    static class SampleAuditable {

        private final String id;
        private final boolean deleted;
        private final Instant deletedAt;
        private final boolean active;

        SampleAuditable(String id, boolean deleted, Instant deletedAt, boolean active) {
            this.id = id;
            this.deleted = deleted;
            this.deletedAt = deletedAt;
            this.active = active;
        }

        public String getId() {
            return id;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public Instant getDeletedAt() {
            return deletedAt;
        }

        public boolean isActive() {
            return active;
        }
    }

    record RelatedDetail(String id, String label) {
    }

    @Nested
    @DisplayName("Extraction Tests (extractIds & extractList)")
    class ExtractionTests {

        @Test
        @DisplayName("Should filter nulls and blanks, and preserve discovery order in LinkedHashSet")
        void extractIds_shouldFilterNullsAndBlanks_andPreserveOrder() {
            List<SampleItem> items = List.of(
                new SampleItem("A", "cat1", "Item A", BigDecimal.ONE, "active"),
                new SampleItem("B", "cat1", "Item B", BigDecimal.ONE, "active"),
                new SampleItem("", "cat1", "Item Blank", BigDecimal.ONE, "active"),
                new SampleItem("   ", "cat1", "Item Spaces", BigDecimal.ONE, "active"),
                new SampleItem("A", "cat1", "Item A Duplicate", BigDecimal.ONE, "active"),
                new SampleItem("C", "cat1", "Item C", BigDecimal.ONE, "active"));

            Set<String> ids = Lookups.extractIds(items, SampleItem::id);

            assertThat(ids).containsExactly("A", "B", "C");
        }

        @Test
        @DisplayName("Should return empty set when input collection is null or empty")
        void extractIds_shouldReturnEmptySet_whenInputNullOrEmpty() {
            assertThat(Lookups.extractIds(null, SampleItem::id)).isEmpty();
            assertThat(Lookups.extractIds(Collections.emptyList(), SampleItem::id))
                .isEmpty();
        }

        @Test
        @DisplayName("Should aggregate IDs across multiple extractors")
        void extractAllIds_shouldCombineMultipleExtractors() {
            record Pair(String id1, String id2) {
            }
            List<Pair> pairs = List.of(new Pair("1", "2"), new Pair("2", "3"), new Pair("", null));

            Set<String> ids = Lookups.extractAllIds(pairs, Pair::id1, Pair::id2);

            assertThat(ids).containsExactly("1", "2", "3");
        }

        @Test
        @DisplayName("Should combine multiple ID collections without duplicates")
        void combineIds_shouldMergeCollections() {
            Set<String> combined = Lookups.combineIds(List.of("A", "B"), Set.of("B", "C"), null);
            assertThat(combined).containsExactly("A", "B", "C");
        }

        @Test
        @DisplayName("Should extract ordered list preserving collection sequence")
        void extractList_shouldPreserveSequence() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "cat1", "First", BigDecimal.TEN, "active"),
                new SampleItem("2", "cat1", "Second", BigDecimal.ONE, "active"));

            List<String> names = Lookups.extractList(items, SampleItem::name);
            assertThat(names).containsExactly("First", "Second");
        }
    }

    @Nested
    @DisplayName("Distinct Tests (distinctBy)")
    class DistinctTests {

        @Test
        @DisplayName("Should deduplicate items by key preserving first occurrence order")
        void distinctBy_shouldKeepFirstOccurrence() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "electronics", "Laptop", BigDecimal.TEN, "active"),
                new SampleItem("2", "clothing", "Shirt", BigDecimal.ONE, "active"),
                new SampleItem("3", "electronics", "Phone", BigDecimal.TEN, "active"));

            List<SampleItem> distinct = Lookups.distinctBy(items, SampleItem::category);

            assertThat(distinct).hasSize(2);
            assertThat(distinct.get(0).name()).isEqualTo("Laptop");
            assertThat(distinct.get(1).name()).isEqualTo("Shirt");
        }
    }

    @Nested
    @DisplayName("Indexing Tests (toMap)")
    class IndexingTests {

        @Test
        @DisplayName("Should index items into LinkedHashMap and safely handle duplicate keys")
        void toMap_shouldPreserveFirstOccurrenceOnDuplicateKeys() {
            List<SampleItem> items = List.of(
                new SampleItem("K1", "c1", "FirstVal", BigDecimal.ONE, "active"),
                new SampleItem("K1", "c1", "SecondVal", BigDecimal.TEN, "active"),
                new SampleItem("K2", "c2", "ThirdVal", BigDecimal.ONE, "active"));

            Map<String, SampleItem> map = Lookups.toMap(items, SampleItem::id);

            assertThat(map).hasSize(2);
            assertThat(map.get("K1").name()).isEqualTo("FirstVal");
            assertThat(map.get("K2").name()).isEqualTo("ThirdVal");
        }

        @Test
        @DisplayName("Should index with custom key and value extractors")
        void toMap_withCustomKeyValueExtractors() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "c1", "Alpha", BigDecimal.ONE, "active"),
                new SampleItem("2", "c2", "Beta", BigDecimal.TEN, "active"));

            Map<String, String> map = Lookups.toMap(items, SampleItem::id, SampleItem::name);

            assertThat(map).containsEntry("1", "Alpha").containsEntry("2", "Beta");
        }
    }

    @Nested
    @DisplayName("Grouping Tests (groupingBy)")
    class GroupingTests {

        @Test
        @DisplayName("Should group items by key preserving insertion order")
        void groupingBy_shouldGroupIntoLists() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "catA", "Item 1", BigDecimal.ONE, "active"),
                new SampleItem("2", "catB", "Item 2", BigDecimal.TEN, "active"),
                new SampleItem("3", "catA", "Item 3", BigDecimal.TEN, "active"));

            Map<String, List<SampleItem>> grouped = Lookups.groupingBy(items, SampleItem::category);

            assertThat(grouped).containsOnlyKeys("catA", "catB");
            assertThat(grouped.get("catA")).extracting(SampleItem::name).containsExactly("Item 1", "Item 3");
            assertThat(grouped.get("catB")).extracting(SampleItem::name).containsExactly("Item 2");
        }
    }

    @Nested
    @DisplayName("Aggregation Tests (summingBy, countingBy, partitionBy, joining)")
    class AggregationTests {

        @Test
        @DisplayName("Should sum BigDecimals accurately without precision loss")
        void summingBy_shouldSumBigDecimals() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "catA", "A", new BigDecimal("10.50"), "active"),
                new SampleItem("2", "catA", "B", new BigDecimal("5.25"), "active"),
                new SampleItem("3", "catB", "C", new BigDecimal("100.00"), "active"));

            Map<String, BigDecimal> sumMap = Lookups.summingBy(items, SampleItem::category, SampleItem::price);

            assertThat(sumMap.get("catA")).isEqualByComparingTo("15.75");
            assertThat(sumMap.get("catB")).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Should count elements by key")
        void countingBy_shouldCountElements() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "catA", "A", BigDecimal.ONE, "active"),
                new SampleItem("2", "catA", "B", BigDecimal.ONE, "active"),
                new SampleItem("3", "catB", "C", BigDecimal.ONE, "active"));

            Map<String, Long> countMap = Lookups.countingBy(items, SampleItem::category);

            assertThat(countMap).containsEntry("catA", 2L).containsEntry("catB", 1L);
        }

        @Test
        @DisplayName("Should partition items into true and false buckets")
        void partitionBy_shouldSeparateLists() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "c", "Active Item", BigDecimal.ONE, "active"),
                new SampleItem("2", "c", "Inactive Item", BigDecimal.ONE, "inactive"));

            Map<Boolean, List<SampleItem>> partitioned = Lookups.partitionBy(items,
                item -> "active".equals(item.status()));

            assertThat(partitioned.get(Boolean.TRUE))
                .extracting(SampleItem::name)
                .containsExactly("Active Item");
            assertThat(partitioned.get(Boolean.FALSE))
                .extracting(SampleItem::name)
                .containsExactly("Inactive Item");
        }

        @Test
        @DisplayName("Should join strings with delimiter discarding blank values")
        void joining_shouldConcatenateNonBlanks() {
            List<String> list = List.of("Apple", "", "Banana", "   ", "Cherry");
            String result = Lookups.joining(list, s -> s, ", ");
            assertThat(result).isEqualTo("Apple, Banana, Cherry");
        }
    }

    @Nested
    @DisplayName("Domain Filtering Tests (notDeleted & activeOnly)")
    class DomainFilteringTests {

        @Test
        @DisplayName("Should filter out soft-deleted items via reflection")
        void notDeleted_shouldFilterSoftDeleted() {
            List<SampleAuditable> list = List.of(
                new SampleAuditable("1", false, null, true),
                new SampleAuditable("2", true, null, true),
                new SampleAuditable("3", false, Instant.now(), true),
                new SampleAuditable("4", false, null, true));

            List<SampleAuditable> result = Lookups.notDeleted(list);

            assertThat(result).extracting(SampleAuditable::getId).containsExactly("1", "4");
        }

        @Test
        @DisplayName("Should filter active items via status string or boolean")
        void activeOnly_shouldFilterActive() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "c", "A", BigDecimal.ONE, "active"),
                new SampleItem("2", "c", "B", BigDecimal.ONE, "inactive"),
                new SampleItem("3", "c", "C", BigDecimal.ONE, "ACTIVE"));

            List<SampleItem> activeItems = Lookups.activeOnly(items, SampleItem::status);

            assertThat(activeItems).extracting(SampleItem::name).containsExactly("A", "C");
        }
    }

    @Nested
    @DisplayName("Safe DTO Resolution Tests (find, resolve, map)")
    class SafeResolutionTests {

        @Test
        @DisplayName("Should safely find values or return fallback default")
        void find_shouldReturnFoundOrDefault() {
            Map<String, String> map = Map.of("k1", "v1");

            assertThat(Lookups.find(map, "k1")).isEqualTo("v1");
            assertThat(Lookups.find(map, "k2")).isNull();
            assertThat(Lookups.find(map, "k2", "fallback")).isEqualTo("fallback");
        }

        @Test
        @DisplayName("Should safely resolve single entity unwrapping Optional")
        void resolve_shouldUnwrapOptional() {
            String resolved = Lookups.resolve("key1", id -> Optional.of("Found: " + id));
            assertThat(resolved).isEqualTo("Found: key1");

            String emptyResolved = Lookups.resolve("key2", id -> Optional.empty());
            assertThat(emptyResolved).isNull();

            String nullIdResolved = Lookups.resolve(null, id -> Optional.of("test"));
            assertThat(nullIdResolved).isNull();
        }

        @Test
        @DisplayName("Should map items joining with lookup map via BiFunction")
        void map_withLookupMapAndBiFunction() {
            List<SampleItem> items = List.of(new SampleItem("1", "c1", "Laptop", BigDecimal.TEN, "active"));
            Map<String, RelatedDetail> detailMap = Map.of("c1", new RelatedDetail("c1", "Electronics Category"));

            List<String> combined = Lookups.map(
                items,
                SampleItem::category,
                detailMap,
                (item, detail) -> item.name() + " -> " + (detail != null ? detail.label() : "Unknown"));

            assertThat(combined).containsExactly("Laptop -> Electronics Category");
        }
    }

    @Nested
    @DisplayName("Fluent Pipeline Tests (Lookups.of)")
    class FluentPipelineTests {

        @Test
        @DisplayName("Should execute fluent chain: extractIds -> loadMap -> toMap")
        void fluentPipeline_shouldWorkSeamlessly() {
            List<SampleItem> items = List.of(
                new SampleItem("1", "c1", "A", BigDecimal.ONE, "active"),
                new SampleItem("2", "c2", "B", BigDecimal.ONE, "active"),
                new SampleItem("3", "c1", "C", BigDecimal.ONE, "active"));

            // Mock external batch loader
            Function<Collection<String>, Collection<RelatedDetail>> batchLoader = ids -> ids.stream()
                .map(id -> new RelatedDetail(id, "Label: " + id))
                .toList();

            Map<String, RelatedDetail> detailMap = Lookups.of(items)
                .extractIds(SampleItem::category)
                .loadMap(batchLoader, RelatedDetail::id);

            assertThat(detailMap).containsOnlyKeys("c1", "c2");
            assertThat(detailMap.get("c1").label()).isEqualTo("Label: c1");
            assertThat(detailMap.get("c2").label()).isEqualTo("Label: c2");
        }
    }
}
