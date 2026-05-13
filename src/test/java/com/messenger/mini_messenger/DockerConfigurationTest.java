package com.messenger.mini_messenger;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerConfigurationTest {

    @Test
    void composeDefinesBackendWithMysqlAndRedisServiceHosts() throws IOException {
        String compose = Files.readString(Path.of("compose.yaml"));

        assertTrue(compose.contains("backend:"), "compose.yaml must define a backend service");
        assertTrue(compose.contains("DB_HOST=mysql"), "backend container must connect to mysql service host");
        assertTrue(compose.contains("REDIS_HOST=redis"), "backend container must connect to redis service host");
        assertTrue(compose.contains("Dockerfile"), "backend service must build from Dockerfile");
    }

    @Test
    void redisCanRunWithOptionalPasswordFromEnv() throws IOException {
        String compose = Files.readString(Path.of("compose.yaml"));

        assertTrue(compose.contains("requirepass"), "Redis command should use requirepass when REDIS_PASSWORD is set");
        assertTrue(compose.contains("REDIS_PASSWORD"), "Redis password must be controlled by REDIS_PASSWORD");
    }

    @Test
    void dockerfileBuildsSpringBootJar() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertTrue(dockerfile.contains("mvnw"), "Dockerfile should build with Maven wrapper");
        assertTrue(dockerfile.contains("java"), "Dockerfile should run the Spring Boot jar with java");
    }
}
