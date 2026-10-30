package com.hyperdata.proms.module.sample.product.facade.internal;

import com.hyperdata.proms.module.sample.product.facade.ProductFacade;
import com.hyperdata.proms.module.sample.product.facade.dto.ProductSummaryDto;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DefaultProductFacade implements ProductFacade {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSummaryDto> findProductSummaryById(UUID id) {
        return productRepository.findById(id).map(productMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStockAvailable(UUID id, int quantity) {
        return productRepository
                .findById(id)
                .map(p -> p.isActive() && p.getStock() >= quantity)
                .orElse(false);
    }
}
