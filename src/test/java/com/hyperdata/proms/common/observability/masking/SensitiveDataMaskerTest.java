package com.hyperdata.proms.common.observability.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    @Test
    @DisplayName("Should mask credentials like password, token, and secret")
    void shouldMaskCredentials() {
        String log = "{\"username\": \"john\", \"password\": \"secret123\", \"token\": \"abcXYZ123\"}";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("\"password\": \"***MASKED***\"");
        assertThat(masked).contains("\"token\": \"***MASKED***\"");
        assertThat(masked).contains("\"username\": \"john\"");
    }

    @Test
    @DisplayName("Should mask credit card preserving only last 4 digits")
    void shouldMaskCreditCard() {
        String log = "Payment processed with card: 4111222233334444 and cvv: 123";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("****-****-****-4444");
        assertThat(masked).contains("***MASKED_CVV***");
    }

    @Test
    @DisplayName("Should mask Indonesian NIK and NPWP")
    void shouldMaskIndonesianIdentities() {
        String log = "User NIK: 3201234567890123 and NPWP: 01.234.567.8-901.000";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("3201************");
        assertThat(masked).contains("01.***.***.*-***.000");
    }

    @Test
    @DisplayName("Should mask phone numbers and email addresses")
    void shouldMaskPhoneAndEmail() {
        String log = "Contact: budi.santoso@hyperdata.com or phone: +6281234567890";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("b****o@hyperdata.com");
        assertThat(masked).contains("+62812******90");
    }

    @Test
    @DisplayName("Should mask raw JWT tokens and PEM private keys")
    void shouldMaskJwtAndPemKey() {
        String jwt = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String pem = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0\n-----END RSA PRIVATE KEY-----";

        String maskedJwt = SensitiveDataMasker.mask(jwt);
        String maskedPem = SensitiveDataMasker.mask(pem);

        assertThat(maskedJwt).contains("eyJ***MASKED_JWT***");
        assertThat(maskedPem).isEqualTo("[MASKED_PRIVATE_KEY]");
    }

    @Test
    @DisplayName("Should respect extensible custom keys")
    void shouldMaskCustomKeys() {
        String log = "{\"secretQuestion\": \"mother maiden name\", \"pinAtm\": \"1234\"}";
        String masked = SensitiveDataMasker.mask(log, Set.of("secretQuestion", "pinAtm"));
        assertThat(masked).contains("\"secretQuestion\": \"***MASKED***\"");
        assertThat(masked).contains("\"pinAtm\": \"***MASKED***\"");
    }
}
