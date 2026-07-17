package com.koda.platform.shared.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantIdTest {

    @Test
    void createsTenantIdFromUuid() {
        UUID value = UUID.randomUUID();

        TenantId tenantId = TenantId.from(value);

        assertThat(tenantId.value()).isEqualTo(value);
        assertThat(tenantId.toString()).isEqualTo(value.toString());
    }

    @Test
    void createsTenantIdFromString() {
        UUID value = UUID.randomUUID();

        TenantId tenantId = TenantId.fromString(value.toString());

        assertThat(tenantId.value()).isEqualTo(value);
    }

    @Test
    void rejectsBlankTenantIdString() {
        assertThatThrownBy(() -> TenantId.fromString(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tenant id is required");
    }

    @Test
    void rejectsInvalidTenantIdString() {
        assertThatThrownBy(() -> TenantId.fromString("not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}