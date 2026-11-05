# Rule 03: Validation with YAVI & 6 Few-Shot Patterns

We use **YAVI (`am.ik.yavi:yavi`)** for type-safe, lambda-based validation.

## Execution Standard in Service / Controller:
```java
YaviValidator.validateAndThrow(CreateProductRequest.validator, request);
```

> **Naming Standard**: Validator instances embedded inside Request records MUST always be declared as `public static final Validator<T> validator` (camelCase `validator`, bukan uppercase `VALIDATOR`).

---

## 6 Canonical YAVI Patterns (Few-Shot Reference)

### Pattern 1: Basic String, Numeric & Format Validations
```java
public record RegisterUserRequest(String email, String fullName, int age) {
    public static final Validator<RegisterUserRequest> validator = ValidatorBuilder.<RegisterUserRequest>of()
        .constraint(RegisterUserRequest::email, "email", c -> c.notBlank().email().lessThanOrEqual(255))
        .constraint(RegisterUserRequest::fullName, "fullName", c -> c.notBlank().greaterThanOrEqual(3).lessThanOrEqual(100))
        .constraint(RegisterUserRequest::age, "age", c -> c.greaterThanOrEqual(18).message("Must be at least 18 years old"))
        .build();
}
```

### Pattern 2: Nullable / Optional Field with Conditional Validation
If field is present (not null), it must conform to a regex/format:
```java
public record UpdateProfileRequest(String phone) {
    public static final Validator<UpdateProfileRequest> validator = ValidatorBuilder.<UpdateProfileRequest>of()
        .constraintOnCondition((req, ctx) -> req.phone() != null, b -> b
            .constraint(UpdateProfileRequest::phone, "phone", c -> c.pattern("^(\\+62|62|0)8[1-9][0-9]{6,10}$").message("Invalid Indonesian phone number"))
        )
        .build();
}
```

### Pattern 3: Nested Object & List / Collection Validations
```java
public record CreateOrderRequest(AddressDto shippingAddress, List<OrderItemDto> items) {
    public static final Validator<CreateOrderRequest> validator = ValidatorBuilder.<CreateOrderRequest>of()
        .nest(CreateOrderRequest::shippingAddress, "shippingAddress", AddressDto.validator)
        .constraint(CreateOrderRequest::items, "items", c -> c.notEmpty().message("At least 1 item is required"))
        .forEach(CreateOrderRequest::items, "items", OrderItemDto.validator)
        .build();
}
```

### Pattern 4: Cross-Field / Conditional Validation
Field B is required only if Field A has a specific value:
```java
public record PaymentRequest(PaymentMethod method, String bankCode, String accountNumber) {
    public static final Validator<PaymentRequest> validator = ValidatorBuilder.<PaymentRequest>of()
        .constraintOnCondition((req, ctx) -> req.method() == PaymentMethod.BANK_TRANSFER, b -> b
            .constraint(PaymentRequest::bankCode, "bankCode", c -> c.notBlank().message("Bank code is required for bank transfer"))
            .constraint(PaymentRequest::accountNumber, "accountNumber", c -> c.notBlank().message("Account number is required for bank transfer"))
        )
        .build();
}
```

### Pattern 5: Confirmation / Value Equality Check
```java
public record ResetPasswordRequest(String password, String confirmPassword) {
    public static final Validator<ResetPasswordRequest> validator = ValidatorBuilder.<ResetPasswordRequest>of()
        .constraint(ResetPasswordRequest::password, "password", c -> c.notBlank().greaterThanOrEqual(8))
        .predicate(req -> Objects.equals(req.password(), req.confirmPassword()), "confirmPassword", "Passwords do not match")
        .build();
}
```

### Pattern 6: Standard Error Envelope Response
When invalid, `YaviValidator.validateAndThrow()` throws `ValidationException`. `GlobalExceptionHandler` converts it automatically to:
```json
{
  "success": false,
  "status": 400,
  "message": "Validation failed",
  "data": null,
  "errors": {
    "email": "email must be a valid email address",
    "confirmPassword": "Passwords do not match"
  },
  "timestamp": "2026-10-30T10:00:00Z"
}
```
