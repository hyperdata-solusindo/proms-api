package com.hyperdata.proms.module.sample.product.mapper;

import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.ProductListItemResponse;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.client.dto.ProductSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    ProductDetailResponse toDetailResponse(Product product);

    ProductListItemResponse toListItemResponse(Product product);

    @Mapping(target = "isAvailable", expression = "java(product.isActive() && product.getStock() > 0)")
    ProductSummaryDto toSummaryDto(Product product);
}
