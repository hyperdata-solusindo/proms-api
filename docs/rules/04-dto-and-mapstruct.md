# Rule 04: DTOs & MapStruct Standards

## 1. Naming Suffixes
- **`*Request`**: HTTP input received in Controller (e.g. `CreateProductRequest`, `UpdateProductRequest`).
- **`*Response`**: HTTP output sent from Controller (e.g. `ProductDetailResponse`, `ProductListItemResponse`).
- **`*Dto`**: Data exchanged between modules via Facade (e.g. `ProductSummaryDto`).

## 2. Separate Detail vs ListItem Responses
- **`*DetailResponse`**: Full payload for single item retrieval (`GET /api/v1/resource/{id}`).
- **`*ListItemResponse`**: Lean payload for paginated tables (`GET /api/v1/resource`).

## 3. MapStruct Configuration Standards
Place mapper interfaces in `module.<domain>.<feature>.mapper.<Feature>Mapper.java`.

```java
package com.hyperdata.proms.module.sample.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    ProductDetailResponse toDetailResponse(Product product);
    ProductListItemResponse toListItemResponse(Product product);
    ProductSummaryDto toSummaryDto(Product product);
}
```

### Static Factory Alternative
For simple records with 1–3 properties, writing `public static MyResponse fromEntity(MyEntity e)` inside the Record is permitted.
