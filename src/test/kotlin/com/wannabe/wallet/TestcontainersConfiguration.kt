package com.wannabe.wallet

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.MountableFile
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer<*> {
        return MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("wallet")
            .withUsername("wallet")
            .withPassword("wallet")
            .withCopyFileToContainer(
                MountableFile.forHostPath("docker/mysql/init/001_create_wallet_schema.sql"),
                "/docker-entrypoint-initdb.d/001_create_wallet_schema.sql",
            )
    }

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(6379)
    }

}
