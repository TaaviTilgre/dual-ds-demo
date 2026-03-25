package com.example.demo.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Configures ShedLock's lock provider.
 *
 * ShedLock prevents scheduled tasks from running simultaneously on multiple instances.
 * It needs a shared storage backend to coordinate locks — here we use a database table
 * (the "shedlock" table created by ShedLockTableInitializer).
 *
 * We explicitly @Qualifier("writeDataSource") to point ShedLock at the PRIMARY database,
 * because locks involve writes (INSERT/UPDATE). Using the read replica would fail or not
 * propagate.
 *
 * usingDbTime() makes ShedLock use the database server's clock instead of the app server's
 * clock — avoids issues when app instances have slightly different system times.
 */
@Configuration
class ShedLockConfig {

    @Bean
    fun lockProvider(@Qualifier("writeDataSource") dataSource: DataSource): LockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(org.springframework.jdbc.core.JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        )
}
