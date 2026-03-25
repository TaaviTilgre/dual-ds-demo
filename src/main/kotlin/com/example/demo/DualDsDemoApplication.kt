package com.example.demo

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

// @SpringBootApplication — turns on component scanning, auto-configuration, and config properties.
// @EnableScheduling — activates Spring's @Scheduled support so cron/fixed-rate methods actually run.
// @EnableSchedulerLock — activates ShedLock's @SchedulerLock annotation processing.
//   defaultLockAtMostFor = "10m" means if a node crashes mid-task, the lock auto-expires after 10 min
//   so another node can pick it up. You can override per-task with lockAtMostFor on the annotation.
@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class DualDsDemoApplication

fun main(args: Array<String>) {
    runApplication<DualDsDemoApplication>(*args)
}
