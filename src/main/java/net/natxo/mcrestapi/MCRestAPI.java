package net.natxo.mcrestapi;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.natxo.mcrestapi.collectors.EventCollector;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.collectors.ServerEvent;
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
		EventCollector eventCollector = new EventCollector();

		ServerTickEvents.END_SERVER_TICK.register(tpsCollector::onEndTick);

		// Chat events
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			eventCollector.push(ServerEvent.chat(
					sender.getGameProfile().name(),
					message.signedContent()
			));
		});

		ServerMessageEvents.COMMAND_MESSAGE.register((message, source, params) -> {
			String playerName = source.getEntity() instanceof ServerPlayer player
					? player.getGameProfile().name()
					: "Server";
			eventCollector.push(ServerEvent.command(playerName, message.signedContent()));
		});

		ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
			if (!overlay) {
				eventCollector.push(ServerEvent.game(message.getString()));
			}
		});

		// Player connection events
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			eventCollector.push(ServerEvent.join(handler.getPlayer().getGameProfile().name()));
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			eventCollector.push(ServerEvent.leave(handler.getPlayer().getGameProfile().name()));
		});

		// Death events
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				String deathMessage = player.getCombatTracker().getDeathMessage().getString();
				eventCollector.push(ServerEvent.death(player.getGameProfile().name(), deathMessage));
			}
		});

		// SSE keepalive ping every 30 seconds
		final int[] tickCounter = {0};
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter[0]++;
			if (tickCounter[0] >= 600) { // 30s at 20 TPS
				tickCounter[0] = 0;
				eventCollector.sendPingToAll();
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			playerTracker.setServer(server);

			if (server instanceof DedicatedServer dedicatedServer) {
				try {
					apiServer = new ApiServer(config, tpsCollector, playerTracker, eventCollector, dedicatedServer);
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
			eventCollector.closeAll();
			playerTracker.setServer(null);
		});

		LOGGER.info("[MCRestAPI] Events registered, waiting for server start...");
	}
}
