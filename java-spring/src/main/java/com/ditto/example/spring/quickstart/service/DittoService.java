package com.ditto.example.spring.quickstart.service;

import com.ditto.example.spring.quickstart.configuration.DittoConfigurationKeys;
import com.ditto.example.spring.quickstart.configuration.DittoSecretsConfiguration;
import com.ditto.java.*;
import com.ditto.java.transports.DittoTransportConfig;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Component
public class DittoService implements DisposableBean {
    private static final String DITTO_SYNC_STATE_COLLECTION = "spring_sync_state";
    private static final String DITTO_SYNC_STATE_ID = "sync_state";

    @Nonnull
    private final Ditto ditto;

    @Nonnull
    private final DittoPresenceObserver presenceObserver;

    @Nonnull
    private final DittoStoreObserver syncStateObserver;

    @Nonnull
    private final Sinks.Many<Boolean> mutableSyncStatePublisher = Sinks.many().replay().latestOrDefault(false);

    private final Logger logger = LoggerFactory.getLogger(DittoService.class);

    DittoService(@Nonnull final Environment environment) {
        try {
            File dittoDir = new File(environment.getRequiredProperty(DittoConfigurationKeys.DITTO_DIR));
            dittoDir.mkdirs();

            DittoDependencies dependencies = new DefaultDittoDependencies(dittoDir);

            /*
             *  Setup Ditto Identity
             *  https://docs.ditto.live/sdk/latest/install-guides/java#integrating-and-initializing
             */
            DittoIdentity identity = new DittoIdentity.OnlinePlayground(
                    DittoSecretsConfiguration.DITTO_APP_ID,
                    DittoSecretsConfiguration.DITTO_PLAYGROUND_TOKEN,
                    // This is required to be set to false to use the correct URLs
                    false,
                    DittoSecretsConfiguration.DITTO_AUTH_URL
            );

            this.ditto = new Ditto.Builder(dependencies)
                    .setIdentity(identity)
                    .build();

            this.ditto.setDeviceName("Spring Java");

            // disable sync with v3 peers, required for DQL
            this.ditto.disableSyncWithV3();

            this.ditto.updateTransportConfig(config -> {
                config.connect(connect -> {
                    // Set the Ditto Websocket URL
                    connect.websocketUrls().add(DittoSecretsConfiguration.DITTO_WEBSOCKET_URL);
                });

                logger.info("Transport config: {}", config);
            });

            presenceObserver = observePeersPresence();

            syncStateObserver = setupAndObserveSyncState();
        } catch (DittoError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Ditto is being closed");

        presenceObserver.close();
        syncStateObserver.close();
        ditto.close();
    }

    @NotNull
    public Ditto getDitto() {
        return ditto;
    }

    public Flux<Boolean> getSyncState() {
        return mutableSyncStatePublisher.asFlux();
    }

    public void toggleSync() {
        try {
            boolean currentSyncState = mutableSyncStatePublisher.asFlux().blockFirst();
            setSyncStateIntoDittoStore(!currentSyncState);
        } catch (DittoError e) {
            throw new RuntimeException(e);
        }
    }

    private DittoPresenceObserver observePeersPresence() {
        return ditto.getPresence().observe((graph) -> {
            logger.info("Peers connected: {}", graph.getRemotePeers().size());
            for (DittoPeer peer : graph.getRemotePeers()) {
                logger.info("Peer: {}", peer.getDeviceName());
                for (DittoConnection connection : peer.getConnections()) {
                    logger.info("\t- {} {} {}", connection.getId(), connection.getConnectionType(), connection.getApproximateDistanceInMeters());
                }
            }
        });
    }

    private DittoStoreObserver setupAndObserveSyncState() {
        try {
            boolean hasNoSyncState = ditto.getStore().execute(
                    "SELECT * FROM %s".formatted(DITTO_SYNC_STATE_COLLECTION)
            ).toCompletableFuture().join().getItems().isEmpty();
            if (hasNoSyncState) {
                ditto.getStore().execute(
                        "INSERT INTO %s DOCUMENTS(:sync)".formatted(DITTO_SYNC_STATE_COLLECTION),
                        Map.of("sync", Map.of("_id", DITTO_SYNC_STATE_ID, DITTO_SYNC_STATE_ID, false))
                ).toCompletableFuture().join();
            }

            return ditto.getStore().registerObserver(
                    "SELECT * FROM %s WHERE _id = :id".formatted(DITTO_SYNC_STATE_COLLECTION),
                    Map.of("id",  DITTO_SYNC_STATE_ID),
                    (result) -> {
                        List<? extends DittoQueryResultItem> items = result.getItems();
                        boolean newSyncState = false;
                        if (!items.isEmpty()) {
                            Map<String, ?> value = items.get(0).getValue();
                            String stringValue = value.get(DITTO_SYNC_STATE_ID).toString();
                            newSyncState = Boolean.parseBoolean(stringValue);
                        }

                        if (newSyncState) {
                            try {
                                ditto.startSync();
                            } catch (DittoError e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            ditto.stopSync();
                        }

                        mutableSyncStatePublisher.tryEmitNext(newSyncState);
                    });
        } catch (DittoError e) {
            throw new RuntimeException(e);
        }
    }

    private void setSyncStateIntoDittoStore(boolean newState) throws DittoError {
        CompletionStage<DittoQueryResult> future = ditto.getStore().execute(
                "UPDATE %s SET %s = :syncState".formatted(DITTO_SYNC_STATE_COLLECTION, DITTO_SYNC_STATE_ID),
                Map.of("syncState", newState)
        );

        try {
            future.toCompletableFuture().join().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
