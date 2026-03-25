package com.example.demo.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Manual datasource configuration — replaces Spring Boot's single-datasource auto-config.
 *
 * Why is this needed?
 *   Spring Boot auto-configures ONE DataSource, ONE EntityManagerFactory, and ONE
 *   TransactionManager from spring.datasource.* properties. The moment you need two
 *   datasources, auto-config can't help — you have to wire the same beans yourself.
 *
 * What this class sets up (mirrors what auto-config does behind the scenes):
 *   1. Two sets of DataSourceProperties + HikariDataSource beans (write & read pools)
 *   2. A RoutingDataSource that wraps them and picks one per-request
 *   3. An EntityManagerFactory (Hibernate) pointing at the routing datasource
 *   4. A JpaTransactionManager tied to that EntityManagerFactory
 *
 * @EnableJpaRepositories tells Spring Data where to find Repository interfaces and
 * which EntityManagerFactory + TransactionManager they should use. In single-datasource
 * projects this annotation is optional (auto-config registers it for you), but with
 * manual config we must be explicit.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.demo.repository"],
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
class DataSourceConfig {

    // ─── WRITE datasource ────────────────────────────────────────────────

    /**
     * Binds spring.datasource.write.url, .driver-class-name, .username, .password
     * from application.yml into a DataSourceProperties holder object.
     * This is the same thing auto-config does with spring.datasource.* — just at a
     * different property path.
     */
    @Bean
    @ConfigurationProperties("spring.datasource.write")
    fun writeDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    /**
     * Creates the HikariCP connection pool for the WRITE (primary) database.
     *
     * @ConfigurationProperties("spring.datasource.write.hikari") maps pool-specific
     * settings (maximum-pool-size, minimum-idle, pool-name, etc.) directly onto the
     * HikariDataSource object.
     *
     * initializeDataSourceBuilder() creates a DataSourceBuilder pre-filled with the
     * url/driver/username/password from writeDataSourceProperties().
     */
    @Bean
    @ConfigurationProperties("spring.datasource.write.hikari")
    fun writeDataSource(): HikariDataSource =
        writeDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()

    // ─── READ datasource ─────────────────────────────────────────────────

    /** Same as writeDataSourceProperties() but reads from spring.datasource.read.* */
    @Bean
    @ConfigurationProperties("spring.datasource.read")
    fun readDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    /** Same as writeDataSource() but creates the READ (replica) pool. */
    @Bean
    @ConfigurationProperties("spring.datasource.read.hikari")
    fun readDataSource(): HikariDataSource =
        readDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()

    // ─── Routing wrapper ─────────────────────────────────────────────────

    /**
     * The routing datasource wraps both real pools behind a single DataSource interface.
     * Everything above it (JPA, Hibernate, your repositories) sees just ONE datasource.
     *
     * @Primary makes this the default DataSource bean — so when any Spring component
     * asks for a DataSource, it gets this router (not the raw write/read pools).
     *
     * setTargetDataSources() registers the lookup map: WRITE→writePool, READ→readPool.
     * setDefaultTargetDataSource() picks write as fallback when no transaction context
     * exists (e.g., during startup schema creation).
     */
    @Primary
    @Bean
    fun routingDataSource(): DataSource {
        val routing = RoutingDataSource()
        routing.setTargetDataSources(
            mapOf<Any, Any>(
                DataSourceType.WRITE to writeDataSource(),
                DataSourceType.READ to readDataSource()
            )
        )
        routing.setDefaultTargetDataSource(writeDataSource())
        return routing
    }

    // ─── JPA / Hibernate ─────────────────────────────────────────────────

    /**
     * EntityManagerFactory — Hibernate's session factory.
     * In single-datasource projects, auto-config creates this for you.
     * Here we point it at our routingDataSource() so Hibernate gets connections through
     * the router. .packages() tells Hibernate where to scan for @Entity classes.
     *
     * hibernate.hbm2ddl.auto = "create-drop" creates tables on startup and drops them
     * on shutdown — only suitable for demos and tests.
     */
    @Primary
    @Bean
    fun entityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(routingDataSource())
            .packages("com.example.demo.entity")
            .persistenceUnit("default")
            .properties(mapOf("hibernate.hbm2ddl.auto" to "create-drop"))
            .build()

    /**
     * TransactionManager — manages @Transactional begin/commit/rollback.
     * In single-datasource projects, auto-config creates this for you.
     * We tie it to our EntityManagerFactory so it manages the same connections.
     */
    @Primary
    @Bean
    fun transactionManager(
        entityManagerFactory: LocalContainerEntityManagerFactoryBean
    ): PlatformTransactionManager =
        JpaTransactionManager(entityManagerFactory.`object`!!)
}
