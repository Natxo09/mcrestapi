package net.natxo.mcrestapi.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelData;
import net.natxo.mcrestapi.http.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class WorldEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private static final Set<String> VALID_FIELDS = Set.of("global", "dimensions");

	private final DedicatedServer server;

	public WorldEndpoint(DedicatedServer server) {
		this.server = server;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		Set<String> fields = parseFields(exchange.getRequestURI().getQuery());

		try {
			CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

			server.execute(() -> {
				Map<String, Object> response = new LinkedHashMap<>();

				if (fields.contains("global")) {
					ServerLevel overworld = server.overworld();
					response.put("global", buildGlobal(overworld));
				}

				if (fields.contains("dimensions")) {
					List<Map<String, Object>> dimensions = new ArrayList<>();
					for (ServerLevel level : server.getAllLevels()) {
						dimensions.add(buildDimension(level));
					}
					response.put("dimensions", dimensions);
				}

				future.complete(response);
			});

			Map<String, Object> response = future.get(5, TimeUnit.SECONDS);
			HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
		} catch (TimeoutException e) {
			HttpUtil.sendJson(exchange, 503, "{\"error\":\"Server tick timeout\"}");
		} catch (Exception e) {
			HttpUtil.sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
		}
	}

	private Set<String> parseFields(String query) {
		if (query == null) {
			return VALID_FIELDS;
		}

		for (String param : query.split("&")) {
			String[] kv = param.split("=", 2);
			if (kv.length == 2 && "fields".equals(kv[0])) {
				return Arrays.stream(kv[1].split(","))
						.filter(VALID_FIELDS::contains)
						.collect(Collectors.toSet());
			}
		}

		return VALID_FIELDS;
	}

	private Map<String, Object> buildGlobal(ServerLevel overworld) {
		LevelData levelData = overworld.getLevelData();
		LevelData.RespawnData respawnData = levelData.getRespawnData();
		BlockPos spawnPos = respawnData.pos();

		Map<String, Object> time = new LinkedHashMap<>();
		time.put("game_time", levelData.getGameTime());
		time.put("day_time", overworld.getOverworldClockTime());
		time.put("day_count", overworld.getOverworldClockTime() / 24000L);

		Map<String, Object> weather = new LinkedHashMap<>();
		weather.put("raining", overworld.isRaining());
		weather.put("thundering", overworld.isThundering());

		Map<String, Object> spawn = new LinkedHashMap<>();
		spawn.put("x", spawnPos.getX());
		spawn.put("y", spawnPos.getY());
		spawn.put("z", spawnPos.getZ());

		Map<String, Object> global = new LinkedHashMap<>();
		global.put("seed", overworld.getSeed());
		global.put("time", time);
		global.put("weather", weather);
		global.put("difficulty", levelData.getDifficulty().getSerializedName());
		global.put("difficulty_locked", levelData.isDifficultyLocked());
		global.put("hardcore", levelData.isHardcore());
		global.put("pvp", overworld.isPvpAllowed());
		global.put("spawn", spawn);

		return global;
	}

	private Map<String, Object> buildDimension(ServerLevel level) {
		WorldBorder border = level.getWorldBorder();

		int entityCount = 0;
		for (var ignored : level.getAllEntities()) {
			entityCount++;
		}

		Map<String, Object> borderData = new LinkedHashMap<>();
		borderData.put("size", border.getSize());
		borderData.put("center_x", border.getCenterX());
		borderData.put("center_z", border.getCenterZ());

		Map<String, Object> dimension = new LinkedHashMap<>();
		dimension.put("name", level.dimension().identifier().toString());
		dimension.put("sea_level", level.getSeaLevel());
		dimension.put("flat", level.isFlat());
		dimension.put("loaded_chunks", level.getChunkSource().getLoadedChunksCount());
		dimension.put("entity_count", entityCount);
		dimension.put("world_border", borderData);

		return dimension;
	}
}
