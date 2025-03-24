package com.ditto.example.spring.quickstart.service;

import com.ditto.example.spring.quickstart.configuration.DittoConfigurationKeys;
import com.ditto.example.spring.quickstart.configuration.DittoEnvironmentConfiguration;
import com.ditto.java.*;
import com.ditto.java.transports.DittoTransportConfig;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.PropertySource;
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
@PropertySource("classpath:secret.properties")
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

    DittoService(@Nonnull final Environment environment) {
        File dittoDir = new File(environment.getRequiredProperty(DittoConfigurationKeys.DITTO_DIR));

        dittoDir.mkdirs();

        DittoDependencies dependencies = new DefaultDittoDependencies(dittoDir);

        String configurationName = environment.getRequiredProperty(DittoConfigurationKeys.CONFIGURATION).toLowerCase();
        DittoIdentity identity = switch (configurationName) {
            case "online" ->
                new DittoIdentity.OnlinePlayground(
                        environment.getRequiredProperty(DittoEnvironmentConfiguration.DITTO_APP_ID),
                        environment.getRequiredProperty(DittoEnvironmentConfiguration.DITTO_PLAYGROUND_TOKEN),
                        true,
                        environment.getRequiredProperty(DittoEnvironmentConfiguration.DITTO_AUTH_URL)
                );
            case "offline" ->
                new DittoIdentity.OfflinePlayground(
                        environment.getRequiredProperty(DittoEnvironmentConfiguration.DITTO_APP_ID)
                );
            default ->
                throw new IllegalArgumentException("%s requires to be set to 'online' or 'offline'".formatted(DittoConfigurationKeys.CONFIGURATION));
        };

        this.ditto = new Ditto.Builder(dependencies)
                .setIdentity(identity)
                .build();

        if (configurationName.equals("offline")) {
            try {
                this.ditto.setOfflineOnlyLicenseToken(
                        environment.getRequiredProperty(DittoConfigurationKeys.IDENTITY_OFFLINE_LICENSE_KEY)
                );
            } catch (DittoError e) {
                throw new RuntimeException(e);
            }
        }

        this.ditto.setDeviceName("Spring Java");
        try {
            this.ditto.disableSyncWithV3();
        } catch (DittoError e) {
            throw new RuntimeException(e);
        }

        DittoTransportConfig transportConfig = new DittoTransportConfig.Builder()
                .peerToPeer(peerToPeer -> {
                    peerToPeer.bluetoothLe().isEnabled(true);

                    peerToPeer.lan().isEnabled(true);

                    peerToPeer.wifiAware().isEnabled(true);
                })
                .listen(listen -> {
                    listen.tcp()
                            .isEnabled(true)
                            .interfaceIp(environment.getRequiredProperty(DittoConfigurationKeys.TRANSPORT_TCP_BASE_ADDRESS))
                            .port(Integer.parseInt(environment.getRequiredProperty(DittoConfigurationKeys.TRANSPORT_TCP_PORT)));
                })
                .connect(connect -> {
                    if (configurationName.equals("online")) {
                        connect.addWebsocketUrls(environment.getRequiredProperty(DittoEnvironmentConfiguration.DITTO_WEBSOCKET_URL));
                    } else {
                        connect.setWebsocketUrls();
                    }
                })
                .build();

        System.out.printf("Transport config: %s%n", transportConfig);

        this.ditto.setTransportConfig(
                transportConfig
        );

        presenceObserver = observePeersPresence();

        syncStateObserver = setupAndObserveSyncState();
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("Ditto is being closed");

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
            System.out.printf("Peers connected: %d%n", graph.getRemotePeers().size());
            for (DittoPeer peer : graph.getRemotePeers()) {
                System.out.printf("Peer: %s%n", peer.getDeviceName());
                for (DittoConnection connection : peer.getConnections()) {
                    System.out.printf("\t- %s %s %s%n", connection.getId(), connection.getConnectionType(), connection.getApproximateDistanceInMeters());
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
