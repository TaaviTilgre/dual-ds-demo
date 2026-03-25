package com.example.demo.config

/**
 * Lookup key used by [RoutingDataSource] to decide which physical datasource to use.
 * The routing datasource holds a Map<DataSourceType, DataSource> and calls
 * determineCurrentLookupKey() on every connection request to pick one.
 */
enum class DataSourceType {
    WRITE, READ
}
