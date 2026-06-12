package net.natxo.mcrestapi.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.collectors.TpsCollector;
import net.natxo.mcrestapi.http.HttpUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerEndpoint implements HttpHandler {

	private static final Gson GSON = new GsonBuilder().create();
	private static final Set<String> VALID_FIELDS = Set.of("info", "tps", "memory", "cpu", "players", "properties", "resource_pack");

	private final TpsCollector tpsCollector;
	private final PlayerTracker playerTracker;
	private final DedicatedServer server;

	public ServerEndpoint(TpsCollector tpsCollector, PlayerTracker playerTracker, DedicatedServer server) {
		this.tpsCollector = tpsCollector;
		this.playerTracker = playerTracker;
		this.server = server;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equals(exchange.getRequestMethod())) {
			HttpUtil.sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
			return;
		}

		Set<String> fields = parseFields(exchange.getRequestURI().getQuery());

		Map<String, Object> response = new LinkedHashMap<>();

		if (fields.contains("info")) {
			response.putAll(buildInfo());
		}
		if (fields.contains("tps")) {
			response.putAll(buildTps());
		}
		if (fields.contains("memory")) {
			response.put("memory", buildMemory());
		}
		if (fields.contains("cpu")) {
			response.put("cpu", buildCpu());
		}
		if (fields.contains("players")) {
			response.put("players", buildPlayers());
		}
		if (fields.contains("properties")) {
			response.put("properties", buildProperties());
		}
		if (fields.contains("resource_pack")) {
			response.put("resource_pack", buildResourcePack());
		}

		HttpUtil.sendJson(exchange, 200, GSON.toJson(response));
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

	private Map<String, Object> buildInfo() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("version", server.getServerVersion());
		info.put("motd", server.getMotd());
		info.put("server_port", server.getPort());
		info.put("online_mode", server.getProperties().onlineMode);
		info.put("uptime_seconds", tpsCollector.getUptimeSeconds());
		return info;
	}

	private Map<String, Object> buildTps() {
		Map<String, Object> tps = new LinkedHashMap<>();
		tps.put("tps", tpsCollector.getTps());
		tps.put("mspt", tpsCollector.getMspt());
		return tps;
	}

	private Map<String, Object> buildMemory() {
		Runtime rt = Runtime.getRuntime();
		long totalMb = rt.totalMemory() / (1024 * 1024);
		long freeMb = rt.freeMemory() / (1024 * 1024);
		long usedMb = totalMb - freeMb;
		long maxMb = rt.maxMemory() / (1024 * 1024);
		double usagePercent = Math.round((double) usedMb / maxMb * 10000.0) / 100.0;

		Map<String, Object> memory = new LinkedHashMap<>();
		memory.put("used_mb", usedMb);
		memory.put("total_mb", totalMb);
		memory.put("max_mb", maxMb);
		memory.put("free_mb", freeMb);
		memory.put("usage_percent", usagePercent);
		return memory;
	}

	private Map<String, Object> buildCpu() {
		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

		Map<String, Object> cpu = new LinkedHashMap<>();
		cpu.put("system_load", osBean.getSystemLoadAverage());
		cpu.put("available_processors", osBean.getAvailableProcessors());

		if (osBean instanceof com.sun.management.OperatingSystemMXBean extBean) {
			double processLoad = Math.round(extBean.getProcessCpuLoad() * 10000.0) / 100.0;
			cpu.put("process_load", processLoad);
		}

		return cpu;
	}

	private Map<String, Object> buildPlayers() {
		Map<String, Object> players = new LinkedHashMap<>();
		players.put("online", playerTracker.getOnlineCount());
		players.put("max", playerTracker.getMaxPlayers());
		return players;
	}

	private Map<String, Object> buildProperties() {
		DedicatedServerProperties props = server.getProperties();

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("gamemode", props.gameMode.get().getName());
		properties.put("difficulty", props.difficulty.get().getSerializedName());
		properties.put("hardcore", props.hardcore);
		properties.put("allow_flight", props.allowFlight.get());
		properties.put("whitelist", props.whiteList.get());
		properties.put("enforce_whitelist", props.enforceWhitelist.get());
		properties.put("enforce_secure_profile", props.enforceSecureProfile);
		properties.put("max_world_size", props.maxWorldSize);
		properties.put("spawn_protection", props.spawnProtection.get());
		properties.put("view_distance", props.viewDistance.get());
		properties.put("simulation_distance", props.simulationDistance.get());
		properties.put("max_tick_time", props.maxTickTime);
		return properties;
	}

	private Object buildResourcePack() {
		Optional<MinecraftServer.ServerResourcePackInfo> packInfo = server.getProperties().serverResourcePackInfo;

		if (packInfo.isEmpty()) {
			return null;
		}

		MinecraftServer.ServerResourcePackInfo info = packInfo.get();

		Map<String, Object> pack = new LinkedHashMap<>();
		pack.put("url", info.url());
		pack.put("hash", info.hash());
		pack.put("required", info.isRequired());

		Component prompt = info.prompt();
		if (prompt != null) {
			pack.put("prompt", prompt.getString());
		}

		return pack;
	}
}
