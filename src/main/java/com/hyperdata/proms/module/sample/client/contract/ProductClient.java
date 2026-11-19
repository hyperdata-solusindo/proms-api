package com.hyperdata.proms.module.sample.client.contract;

import com.hyperdata.proms.module.sample.client.dto.ProductSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductClient {

    Optional<ProductSummaryDto> findProductSummaryById(UUID id);

    List<ProductSummaryDto> findAllByIds(Collection<UUID> ids);

    boolean isStockAvailable(UUID id, int quantity);
}
