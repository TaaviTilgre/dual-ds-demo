package com.example.demo.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Creates the "shedlock" table on the WRITE database at application startup.
 *
 * ShedLock needs a table to store lock records. In production you'd normally create this
 * via a migration tool (Flyway/Liquibase). Here we run the DDL script directly for simplicity.
 *
 * The SQL lives in src/main/resources/schema-shedlock.sql.
 *
 * We @Qualifier("writeDataSource") because the shedlock table must live on the primary DB
 * (same one ShedLockConfig points to).
 *
 * ApplicationRunner runs once after the application context is fully initialised.
 */
@Component
class ShedLockTableInitializer(
    @Qualifier("writeDataSource") private val dataSource: DataSource
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val populator = ResourceDatabasePopulator(ClassPathResource("schema-shedlock.sql"))
        populator.execute(dataSource)
    }
}
