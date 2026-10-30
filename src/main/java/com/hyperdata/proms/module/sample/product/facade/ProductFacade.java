package com.hyperdata.proms.module.sample.product.facade;

import com.hyperdata.proms.module.sample.product.facade.dto.ProductSummaryDto;
import java.util.Optional;
import java.util.UUID;

public interface ProductFacade {
    Optional<ProductSummaryDto> findProductSummaryById(UUID id);

    boolean isStockAvailable(UUID id, int quantity);
}
