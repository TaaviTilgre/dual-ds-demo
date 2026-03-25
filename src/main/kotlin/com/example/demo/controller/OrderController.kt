package com.example.demo.controller

import com.example.demo.entity.Order
import com.example.demo.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller exposing CRUD endpoints for orders.
 *
 * The controller itself has no datasource or circuit breaker logic — it just delegates
 * to OrderService. The routing (read vs write pool) and resilience (circuit breakers)
 * are handled transparently by the service layer annotations.
 *
 * GET  /api/orders       → readOnly transaction → READ datasource
 * GET  /api/orders/{id}  → readOnly transaction → READ datasource
 * POST /api/orders       → read-write transaction → WRITE datasource
 * DELETE /api/orders/{id} → read-write transaction → WRITE datasource
 */
@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @GetMapping
    fun getAll(): List<Order> = orderService.getAllOrders()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Order> =
        orderService.getOrder(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    fun create(@RequestBody request: CreateOrderRequest): Order =
        orderService.createOrder(request.product, request.quantity)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orderService.deleteOrder(id)
        return ResponseEntity.noContent().build()
    }

    data class CreateOrderRequest(val product: String, val quantity: Int)
}
