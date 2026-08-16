package com.user.configs.actuator

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component
import java.sql.SQLException
import javax.sql.DataSource

@Component("postgresql")
class PostgresqlHealthIndicator(
    private val dataSource: DataSource,
) : HealthIndicator {

    override fun health(): Health {
        return try {
            dataSource.connection.use { connection ->
                if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                    Health.up()
                        .withDetail("database", connection.metaData.databaseProductName)
                        .withDetail("version", connection.metaData.databaseProductVersion)
                        .build()
                } else {
                    Health.down()
                        .withDetail("reason", "Connection is not valid")
                        .build()
                }
            }
        } catch (ex: SQLException) {
            Health.down(ex).build()
        }
    }

    private companion object {
        const val VALIDATION_TIMEOUT_SECONDS = 2
    }
}
