package net.natxo.mcrestapi;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.collectors.TpsCollector;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.http.ApiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class MCRestAPI implements DedicatedServerModInitializer {

	public static final String MOD_ID = "mcrestapi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private ApiServer apiServer;

	@Override
	public void onInitializeServer() {
		LOGGER.info("[MCRestAPI] Initializing...");

		Path configDir = FabricLoader.getInstance().getConfigDir();
		ApiConfig config = ApiConfig.loadOrCreate(configDir);

		TpsCollector tpsCollector = new TpsCollector();
		PlayerTracker playerTracker = new PlayerTracker();

		ServerTickEvents.END_SERVER_TICK.register(tpsCollector::onEndTick);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			playerTracker.setServer(server);

			if (server instanceof DedicatedServer dedicatedServer) {
				try {
					apiServer = new ApiServer(config, tpsCollector, playerTracker, dedicatedServer);
					apiServer.start();
				} catch (IOException e) {
					LOGGER.error("[MCRestAPI] Failed to start HTTP server", e);
				}
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (apiServer != null) {
				apiServer.stop();
				apiServer = null;
			}
			playerTracker.setServer(null);
		});

		LOGGER.info("[MCRestAPI] Events registered, waiting for server start...");
	}
}