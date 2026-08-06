package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoManager;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class DittoConfigRestController {

    private final DittoManager dittoManager;

    public DittoConfigRestController(final DittoManager dittoManager) {
        this.dittoManager = dittoManager;
    }

    // Idempotent desired-state endpoints — there is no blind "toggle". A repeated
    // request (retry, stale double-click) cannot reverse the intended result.
    @PostMapping("/ditto/sync/start")
    public String startSync() {
        dittoManager.setSyncEnabled(true);
        return "";
    }

    @PostMapping("/ditto/sync/stop")
    public String stopSync() {
        dittoManager.setSyncEnabled(false);
        return "";
    }

    // Streams the sync control (current state label + the action button that
    // reflects it) so the displayed state and the control always update together.
    @GetMapping(path = "/ditto/sync/state", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> syncState() {
        return dittoManager.getSyncState()
                .map(enabled -> ServerSentEvent.builder(renderSyncControl(enabled))
                    .event("sync_state")
                    .build()
                );
    }

    // Server-rendered control fragment. When sync is enabled the button offers to
    // stop it; when disabled it offers to start it — the label always states the
    // action, never an ambiguous "Toggle". Single line: SSE data must not contain
    // raw newlines.
    private static String renderSyncControl(boolean enabled) {
        if (enabled) {
            return "<span>Sync State: Enabled</span>"
                    + "<button type=\"button\" hx-post=\"/ditto/sync/stop\" hx-swap=\"none\">Stop sync</button>";
        }
        return "<span>Sync State: Disabled</span>"
                + "<button type=\"button\" hx-post=\"/ditto/sync/start\" hx-swap=\"none\">Start sync</button>";
    }
}
