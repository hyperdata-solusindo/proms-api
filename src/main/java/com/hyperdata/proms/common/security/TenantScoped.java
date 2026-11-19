package com.hyperdata.proms.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Menandai method atau class yang wajib dieksekusi dalam konteks tenant tertentu.
 * Jika activeTenantId bernilai null, sistem akan menolak request (Fail-Closed, HTTP 403 Forbidden).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantScoped {
}
