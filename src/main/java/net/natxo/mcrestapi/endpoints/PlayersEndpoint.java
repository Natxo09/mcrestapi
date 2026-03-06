package net.natxo.mcrestapi.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayersEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();

	private final PlayerTracker playerTracker;

	public PlayersEndpoint(PlayerTracker playerTracker) {
		this.playerTracker = playerTracker;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		try {
			List<PlayerTracker.PlayerSnapshot> snapshots = playerTracker.getPlayerSnapshots()
					.get(5, TimeUnit.SECONDS);

			List<Map<String, Object>> playerList = snapshots.stream()
					.map(this::snapshotToMap)
					.toList();

			Map<String, Object> response = new LinkedHashMap<>();
			response.put("count", playerList.size());
			response.put("players", playerList);

			HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
		} catch (TimeoutException e) {
			HttpUtil.sendJson(exchange, 503, "{\"error\":\"Server tick timeout\"}");
		} catch (Exception e) {
			HttpUtil.sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
		}
	}

	private Map<String, Object> snapshotToMap(PlayerTracker.PlayerSnapshot snapshot) {
		Map<String, Object> position = new LinkedHashMap<>();
		position.put("x", snapshot.x());
		position.put("y", snapshot.y());
		position.put("z", snapshot.z());

		Map<String, Object> player = new LinkedHashMap<>();
		player.put("name", snapshot.name());
		player.put("uuid", snapshot.uuid());
		player.put("health", snapshot.health());
		player.put("food_level", snapshot.foodLevel());
		player.put("position", position);
		player.put("dimension", snapshot.dimension());
		player.put("ping_ms", snapshot.pingMs());
		player.put("game_mode", snapshot.gameMode());
		player.put("is_op", snapshot.isOp());

		return player;
	}
}
