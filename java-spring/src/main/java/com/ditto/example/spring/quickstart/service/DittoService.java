package com.ditto.example.spring.quickstart.service;

import com.ditto.example.spring.quickstart.configuration.DittoConfigurationKeys;
import com.ditto.example.spring.quickstart.configuration.DittoSecretsConfiguration;
import com.ditto.java.*;
import com.ditto.java.serialization.DittoCborSerializable;
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
import java.util.concurrent.CompletionStage;

@Component
public class DittoService implements DisposableBean {
    private static final String DITTO_SYNC_STATE_COLLECTION = "spring_sync_state";
    private static final String DITTO_SYNC_STATE_ID = "sync_state";

    @Nonnull
    private final Ditto ditto;

    @Nonnull
    private final DittoAsyncCancellable presenceObserver;

    @Nonnull
    private final DittoStoreObserver syncStateObserver;

    @Nonnull
    private final Sinks.Many<Boolean> mutableSyncStatePublisher = Sinks.many().replay().latestOrDefault(false);

    private final Logger logger = LoggerFactory.getLogger(DittoService.class);

    DittoService(@Nonnull final Environment environment) {
        File dittoDir = new File(environment.getRequiredProperty(DittoConfigurationKeys.DITTO_DIR));
        dittoDir.mkdirs();

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

        DittoConfig dittoConfig = new DittoConfig.Builder(dittoDir)
                .identity(identity)
                .build();

        this.ditto = new Ditto(dittoConfig);

        this.ditto.setDeviceName("Spring Java");

        this.ditto.updateTransportConfig(transportConfig -> {
            transportConfig.connect(connect -> {
                // Set the Ditto Websocket URL
                connect.websocketUrls().add(DittoSecretsConfiguration.DITTO_WEBSOCKET_URL);
            });

            logger.info("Transport config: {}", transportConfig);
        });

        presenceObserver = observePeersPresence();

        syncStateObserver = setupAndObserveSyncState();
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Ditto is being closed");

        presenceObserver.cancel();
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

    private DittoAsyncCancellable observePeersPresence() {
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
                        DittoCborSerializable.buildDictionary()
                                .put("sync", DittoCborSerializable.buildDictionary()
                                        .put("_id", DITTO_SYNC_STATE_ID)
                                        .put(DITTO_SYNC_STATE_ID, false)
                                        .build()
                                )
                                .build()
                ).toCompletableFuture().join();
            }

            return ditto.getStore().registerObserver(
                    "SELECT * FROM %s WHERE _id = :id".formatted(DITTO_SYNC_STATE_COLLECTION),
                    DittoCborSerializable.buildDictionary()
                            .put("id",  DITTO_SYNC_STATE_ID)
                            .build(),
                    (result) -> {
                        List<? extends DittoQueryResultItem> items = result.getItems();
                        boolean newSyncState = false;
                        if (!items.isEmpty()) {
                            newSyncState = items.get(0).getValue()
                                    .get(DITTO_SYNC_STATE_ID)
                                    .getBoolean();
                        }

                        if (newSyncState) {
                            try {
                                ditto.getStore().execute("ALTER SYSTEM SET DQL_STRICT_MODE = false").toCompletableFuture().join();
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
                DittoCborSerializable.buildDictionary()
                        .put("syncState", newState)
                        .build()
        );

        try {
            future.toCompletableFuture().join().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
