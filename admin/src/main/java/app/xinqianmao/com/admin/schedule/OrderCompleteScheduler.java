/**
 * File: OrderCompleteScheduler.java
 * Author: system
 * Date: 2026-06-27
 */
package app.xinqianmao.com.admin.schedule;

import app.xinqianmao.com.admin.service.OrderCompleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task entry point — dispatches daily order auto-completion.
 * Equivalent to a controller in the schedule layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompleteScheduler {

    private final OrderCompleteService orderCompleteService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void executeDaily() {
        log.info("Daily order auto-completion started");
        orderCompleteService.completeOrdersForAllTenants();
    }
}
