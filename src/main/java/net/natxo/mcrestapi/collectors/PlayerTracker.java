package net.natxo.mcrestapi.collectors;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerTracker {

	private volatile MinecraftServer server;

	public void setServer(MinecraftServer server) {
		this.server = server;
	}

	public CompletableFuture<List<PlayerSnapshot>> getPlayerSnapshots() {
		MinecraftServer srv = this.server;
		if (srv == null) {
			return CompletableFuture.completedFuture(List.of());
		}

		CompletableFuture<List<PlayerSnapshot>> future = new CompletableFuture<>();

		srv.execute(() -> {
			List<PlayerSnapshot> snapshots = srv.getPlayerList().getPlayers().stream()
					.map(PlayerSnapshot::fromPlayer)
					.toList();
			future.complete(snapshots);
		});

		return future;
	}

	public int getOnlineCount() {
		MinecraftServer srv = this.server;
		return srv != null ? srv.getPlayerCount() : 0;
	}

	public int getMaxPlayers() {
		MinecraftServer srv = this.server;
		return srv != null ? srv.getMaxPlayers() : 0;
	}

	public record PlayerSnapshot(
			String name,
			String uuid,
			double health,
			int foodLevel,
			double x,
			double y,
			double z,
			String dimension,
			int pingMs,
			String gameMode,
			boolean isOp
	) {
		public static PlayerSnapshot fromPlayer(ServerPlayer player) {
			return new PlayerSnapshot(
					player.getGameProfile().name(),
					player.getStringUUID(),
					player.getHealth(),
					player.getFoodData().getFoodLevel(),
					player.getX(),
					player.getY(),
					player.getZ(),
					player.level().dimension().identifier().toString(),
					player.connection.latency(),
					player.gameMode.getGameModeForPlayer().getName(),
					player.level().getServer().getPlayerList().isOp(new NameAndId(player.getGameProfile()))
			);
		}
	}
}
