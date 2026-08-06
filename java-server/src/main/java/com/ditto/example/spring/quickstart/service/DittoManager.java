package com.ditto.example.spring.quickstart.service;

import com.ditto.example.spring.quickstart.configuration.DittoConfigurationKeys;
import com.ditto.example.spring.quickstart.configuration.DittoMode;
import com.ditto.example.spring.quickstart.configuration.DittoSecretsConfiguration;
import com.ditto.java.Ditto;
import com.ditto.java.DittoAsyncCancellable;
import com.ditto.java.DittoAuthenticationProvider;
import com.ditto.java.DittoConfig;
import com.ditto.java.DittoConnection;
import com.ditto.java.DittoException;
import com.ditto.java.DittoFactory;
import com.ditto.java.DittoPeer;
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

@Component
public class DittoManager implements DisposableBean {
    @Nonnull
    private final Ditto ditto;

    @Nonnull
    private final DittoAsyncCancellable presenceObserver;

    // Initialized to false and only flipped to true after sync().start() succeeds
    // (see setSyncEnabled), so the streamed state reflects sync actually starting
    // rather than a matching default — that keeps the startup regression test honest.
    @Nonnull
    private final Sinks.Many<Boolean> mutableSyncStatePublisher = Sinks.many().replay().latestOrDefault(false);

    // Whether Ditto sync is currently running. Starts enabled on init (see constructor);
    // changed via the REST start/stop endpoints.
    private volatile boolean syncEnabled = false;

    private final Logger logger = LoggerFactory.getLogger(DittoManager.class);

    DittoManager(@Nonnull final Environment environment) {
        File dittoDir = new File(environment.getRequiredProperty(DittoConfigurationKeys.DITTO_DIR));
        dittoDir.mkdirs();

        /*
         *  Setup Ditto Config
         *  https://docs.ditto.live/sdk/latest/install-guides/java#integrating-and-initializing
         */
        String offlineLicenseToken = DittoSecretsConfiguration.DITTO_OFFLINE_LICENSE_TOKEN.trim();
        boolean isOffline = DittoMode.select(offlineLicenseToken) == DittoMode.OFFLINE;
        DittoConfig.Builder configBuilder =
                new DittoConfig.Builder(DittoSecretsConfiguration.DITTO_DATABASE_ID);
        if (isOffline) {
            configBuilder.smallPeersOnlyConnect(null);
        } else {
            configBuilder.serverConnect(DittoSecretsConfiguration.DITTO_SERVER_URL);
        }
        DittoConfig dittoConfig = configBuilder.build();

        this.ditto = DittoFactory.create(dittoConfig);

        if (isOffline) {
            try {
                this.ditto.setOfflineOnlyLicenseToken(offlineLicenseToken);
            } catch (DittoException e) {
                throw new RuntimeException("Failed to activate offline license token", e);
            }
        } else {
            this.ditto.getAuth().setExpirationHandler((expiringDitto, _timeUntilExpiration) ->
                    expiringDitto.getAuth()
                            .login(
                                    DittoSecretsConfiguration.DITTO_DEVELOPMENT_TOKEN,
                                    DittoAuthenticationProvider.development()
                            ).thenRun(() -> { })
            );
        }

        this.ditto.setDeviceName("Java");

        presenceObserver = observePeersPresence();

        // Start sync on initialization so the quickstart syncs out of the box.
        // The UI / REST start/stop endpoints can subsequently stop and restart it.
        setSyncEnabled(true);
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Ditto is being closed");

        presenceObserver.cancel();
        ditto.close();
    }

    @NotNull
    public Ditto getDitto() {
        return ditto;
    }

    public Flux<Boolean> getSyncState() {
        return mutableSyncStatePublisher.asFlux();
    }

    // Set the desired sync state. Idempotent: calling it with the already-active
    // state is a no-op, so a repeated request / retry / stale double-click cannot
    // flip the transport lifecycle. There is deliberately no blind "toggle".
    // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
    public void setSyncEnabled(boolean enabled) {
        if (enabled == syncEnabled) {
            return;
        }
        if (enabled) {
            try {
                ditto.getSync().start();
            } catch (DittoException e) {
                throw new RuntimeException(e);
            }
        } else {
            ditto.getSync().stop();
        }
        syncEnabled = enabled;
        mutableSyncStatePublisher.tryEmitNext(enabled);
    }

    private DittoAsyncCancellable observePeersPresence() {
        return ditto.getPresence().observe((graph) -> {
            logger.info("Peers connected: {}", graph.getRemotePeers().size());
            for (DittoPeer peer : graph.getRemotePeers()) {
                logger.info("Peer: {}", peer.getDeviceName());
                for (DittoConnection connection : peer.getConnections()) {
                    logger.info("\t- {} {}", connection.getId(), connection.getConnectionType());
                }
            }
        });
    }

}
