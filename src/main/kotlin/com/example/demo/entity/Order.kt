package com.example.demo.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Simple JPA entity mapped to the "orders" table.
 *
 * Hibernate discovers this class because DataSourceConfig.entityManagerFactory()
 * specifies .packages("com.example.demo.entity") — any @Entity in that package is picked up.
 *
 * @GeneratedValue(IDENTITY) lets the database auto-increment the primary key.
 */
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var product: String = "",

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
