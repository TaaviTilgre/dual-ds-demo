package com.example.demo.service

import com.example.demo.entity.Order
import com.example.demo.repository.OrderRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service layer that demonstrates both read/write routing and circuit breakers.
 *
 * Datasource routing:
 *   @Transactional(readOnly = true) → RoutingDataSource picks the READ pool
 *   @Transactional                  → RoutingDataSource picks the WRITE pool
 *   That's it — no extra code needed. The routing is automatic.
 *
 * Circuit breakers (Resilience4j):
 *   @CircuitBreaker(name = "readService", fallbackMethod = "...") wraps the method.
 *   If the method keeps failing (e.g., DB is down), the circuit "opens" and calls
 *   go straight to the fallback without hitting the DB — protecting the system from
 *   cascading failures. After a wait period it "half-opens" to test if the DB recovered.
 *
 *   The circuit breaker settings (window size, failure threshold, wait duration) are
 *   configured in application.yml under resilience4j.circuitbreaker.instances.
 *
 * Fallback rules:
 *   - Must have the same return type as the original method
 *   - Must accept the same parameters PLUS a Throwable as the last parameter
 *   - Should return a safe default (empty list, null, stub object, etc.)
 */
@Service
class OrderService(private val orderRepository: OrderRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ─── READ operations (routed to read replica) ────────────────────────

    @CircuitBreaker(name = "readService", fallbackMethod = "getAllOrdersFallback")
    @Transactional(readOnly = true)
    fun getAllOrders(): List<Order> {
        log.info("Reading all orders (routed to READ datasource)")
        return orderRepository.findAll()
    }

    @CircuitBreaker(name = "readService", fallbackMethod = "getOrderFallback")
    @Transactional(readOnly = true)
    fun getOrder(id: Long): Order? {
        log.info("Reading order id={} (routed to READ datasource)", id)
        return orderRepository.findById(id).orElse(null)
    }

    // ─── WRITE operations (routed to primary) ────────────────────────────

    @CircuitBreaker(name = "writeService", fallbackMethod = "createOrderFallback")
    @Transactional
    fun createOrder(product: String, quantity: Int): Order {
        log.info("Creating order (routed to WRITE datasource): product={}, quantity={}", product, quantity)
        return orderRepository.save(Order(product = product, quantity = quantity))
    }

    @CircuitBreaker(name = "writeService", fallbackMethod = "deleteOrderFallback")
    @Transactional
    fun deleteOrder(id: Long) {
        log.info("Deleting order id={} (routed to WRITE datasource)", id)
        orderRepository.deleteById(id)
    }

    // ─── Fallbacks ───────────────────────────────────────────────────────
    // These are called when the circuit breaker is open (too many recent failures).
    // They must match the original method signature + a trailing Throwable parameter.

    @Suppress("unused")
    private fun getAllOrdersFallback(ex: Throwable): List<Order> {
        log.warn("Circuit breaker open for readService. Returning empty list.", ex)
        return emptyList()
    }

    @Suppress("unused")
    private fun getOrderFallback(id: Long, ex: Throwable): Order? {
        log.warn("Circuit breaker open for readService. Returning null.", ex)
        return null
    }

    @Suppress("unused")
    private fun createOrderFallback(product: String, quantity: Int, ex: Throwable): Order {
        log.warn("Circuit breaker open for writeService. Returning stub order.", ex)
        return Order(id = -1, product = product, quantity = quantity)
    }

    @Suppress("unused")
    private fun deleteOrderFallback(id: Long, ex: Throwable) {
        log.warn("Circuit breaker open for writeService. Delete skipped for id={}.", id, ex)
    }
}
