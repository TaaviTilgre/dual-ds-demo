package com.example.demo.config

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * A datasource that delegates to either the WRITE or READ pool depending on the
 * current transaction's readOnly flag.
 *
 * How it works:
 *   1. Spring's AbstractRoutingDataSource wraps multiple real datasources behind a single
 *      DataSource interface. Every time someone asks for a JDBC connection, Spring calls
 *      determineCurrentLookupKey() to decide which real pool to hand out.
 *
 *   2. We check TransactionSynchronizationManager.isCurrentTransactionReadOnly().
 *      This returns true when the enclosing @Transactional has readOnly = true.
 *
 *   3. If readOnly → return READ key → connection comes from the read replica pool.
 *      Otherwise  → return WRITE key → connection comes from the primary/write pool.
 *
 * This means your service code controls routing just by choosing:
 *   @Transactional(readOnly = true)  → read replica
 *   @Transactional                   → primary
 */
class RoutingDataSource : AbstractRoutingDataSource() {

    override fun determineCurrentLookupKey(): DataSourceType =
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            DataSourceType.READ
        } else {
            DataSourceType.WRITE
        }
}
