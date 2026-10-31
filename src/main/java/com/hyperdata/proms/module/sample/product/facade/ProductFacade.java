package com.hyperdata.proms.module.sample.product.facade;

import com.hyperdata.proms.module.sample.product.facade.dto.ProductSummaryDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductFacade {
    Optional<ProductSummaryDto> findProductSummaryById(UUID id);

    List<ProductSummaryDto> findAllByIds(Collection<UUID> ids);

    boolean isStockAvailable(UUID id, int quantity);
}
