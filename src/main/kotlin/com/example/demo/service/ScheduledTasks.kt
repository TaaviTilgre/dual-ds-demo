package com.example.demo.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Example scheduled task protected by ShedLock.
 *
 * @Scheduled(fixedRate = 60_000) — Spring runs this method every 60 seconds.
 *
 * @SchedulerLock — ShedLock ensures only ONE instance across all running nodes
 * executes this task at a time. Without it, if you deploy 3 replicas behind a load
 * balancer, the task would fire 3 times per minute instead of once.
 *
 *   name = "syncOrderStats"  — unique lock name stored in the shedlock DB table
 *   lockAtLeastFor = "30s"   — hold the lock for at least 30s even if the task finishes
 *                               sooner, to prevent a second execution from starting
 *                               immediately on another node
 *   lockAtMostFor = "5m"     — safety net: if the node crashes mid-task and can't release
 *                               the lock, it auto-expires after 5 minutes so other nodes
 *                               can take over
 */
@Component
class ScheduledTasks(private val orderService: OrderService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "syncOrderStats", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    fun syncOrderStats() {
        val orderCount = orderService.getAllOrders().size
        log.info("Scheduled task [syncOrderStats]: current order count = {}", orderCount)
    }
}
