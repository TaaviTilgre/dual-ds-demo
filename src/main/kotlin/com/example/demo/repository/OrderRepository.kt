package com.example.demo.repository

import com.example.demo.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [Order] entities.
 *
 * By extending JpaRepository, Spring auto-generates implementations for findAll(),
 * findById(), save(), deleteById(), etc. — no code needed.
 *
 * This repository is wired to the routingDataSource via DataSourceConfig's
 * @EnableJpaRepositories. It doesn't know or care that there are two physical databases
 * behind it — the routing is transparent.
 */
@Repository
interface OrderRepository : JpaRepository<Order, Long>
