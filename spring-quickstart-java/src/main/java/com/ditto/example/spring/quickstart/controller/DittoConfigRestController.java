package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

@RestController
public class DittoConfigRestController {

    private final DittoService dittoService;

    public DittoConfigRestController(final DittoService dittoService) {
        this.dittoService = dittoService;
    }

    @PostMapping("/ditto/sync/toggle")
    public String toggleSync() {
        dittoService.toggleSync();
        return "";
    }

    @GetMapping(path = "/ditto/sync/state", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> syncState() {
        return dittoService.getSyncState()
                .map(syncState -> ServerSentEvent.builder("Sync State: %s".formatted(syncState))
                    .event("sync_state")
                    .build()
                );
    }
}
