package com.integration.scheduler;

import com.integration.producer.KafkaPublishService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FetchScheduler {

    private final KafkaPublishService service;

    public FetchScheduler(KafkaPublishService service){
        this.service=service;
    }

    @Scheduled(fixedRateString = "${scheduler.rate-ms}")
    public void run(){
        service.publishAll();
    }
}
