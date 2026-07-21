package com.koda.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorHealthPostgresqlIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
        .withDatabaseName("koda_platform_health_it")
        .withUsername("koda")
        .withPassword("koda_dev_password");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("koda.security.jwt.secret", () -> "test-secret-test-secret-test-secret-32");
    }

    @Test
    void readinessIncludesCriticalDependenciesWithoutDetails() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url("/actuator/health/readiness"), JsonNode.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.at("/status").asText()).isEqualTo("UP");
        assertThat(body.at("/components/readinessState/status").asText()).isEqualTo("UP");
        assertThat(body.at("/components/db/status").asText()).isEqualTo("UP");
        assertThat(body.at("/components/kodaSchema/status").asText()).isEqualTo("UP");
        assertThat(body.at("/components/db/details").isMissingNode()).isTrue();
        assertThat(body.at("/components/kodaSchema/details").isMissingNode()).isTrue();
    }

    @Test
    void livenessOnlyIncludesProcessState() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url("/actuator/health/liveness"), JsonNode.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.at("/status").asText()).isEqualTo("UP");
        assertThat(body.at("/components/livenessState/status").asText()).isEqualTo("UP");
        assertThat(body.at("/components/db").isMissingNode()).isTrue();
        assertThat(body.at("/components/flyway").isMissingNode()).isTrue();
        assertThat(body.at("/components/kodaSchema").isMissingNode()).isTrue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
