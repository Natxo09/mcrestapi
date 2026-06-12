package net.natxo.mcrestapi.http;

import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.natxo.mcrestapi.MCRestAPI;
import net.natxo.mcrestapi.collectors.EventCollector;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.collectors.TpsCollector;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.endpoints.AdminDashboardEndpoint;
import net.natxo.mcrestapi.endpoints.ChatEndpoint;
import net.natxo.mcrestapi.endpoints.CommandEndpoint;
import net.natxo.mcrestapi.endpoints.EventStreamEndpoint;
import net.natxo.mcrestapi.endpoints.ModsEndpoint;
import net.natxo.mcrestapi.endpoints.OpenApiEndpoint;
import net.natxo.mcrestapi.endpoints.PlayersEndpoint;
import net.natxo.mcrestapi.endpoints.ServerEndpoint;
import net.natxo.mcrestapi.endpoints.ServerIconEndpoint;
import net.natxo.mcrestapi.endpoints.SwaggerEndpoint;
import net.natxo.mcrestapi.endpoints.WorldEndpoint;
import net.natxo.mcrestapi.endpoints.admin.CorsAdminEndpoint;
import net.natxo.mcrestapi.endpoints.admin.KeysAdminEndpoint;
import net.natxo.mcrestapi.endpoints.admin.SettingsAdminEndpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class ApiServer {

	private final HttpServer httpServer;
	private final boolean swaggerEnabled;
	private final ApiConfig config;

	public ApiServer(ApiConfig config, TpsCollector tpsCollector, PlayerTracker playerTracker, EventCollector eventCollector, DedicatedServer server) throws IOException {
		this.config = config;
		InetSocketAddress address = new InetSocketAddress(config.getBindAddress(), config.getPort());
		this.httpServer = HttpServer.create(address, config.getMaxConnections());
		this.httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		this.swaggerEnabled = config.isSwaggerEnabled();

		Router router = new Router(httpServer, config);
		router.register("/api/server", new ServerEndpoint(tpsCollector, playerTracker, server), "server.read");
		router.register("/api/players", new PlayersEndpoint(playerTracker), "players.read");
		router.register("/api/world", new WorldEndpoint(server), "world.read");
		router.register("/api/chat", new ChatEndpoint(eventCollector), "chat.read");
		router.register("/api/events/stream", new EventStreamEndpoint(eventCollector), "chat.stream");
		router.register("/api/command", new CommandEndpoint(server), "command.execute");
		router.register("/api/mods", new ModsEndpoint(), "mods.read");

		Path serverDir = server.getServerDirectory();
		router.registerPublic("/api/server/icon", new ServerIconEndpoint(serverDir));

		router.registerPublic("/admin", new AdminDashboardEndpoint());

		router.registerAdmin("/api/admin/keys", new KeysAdminEndpoint(config));
		router.registerAdmin("/api/admin/cors", new CorsAdminEndpoint(config));
		router.registerAdmin("/api/admin/settings", new SettingsAdminEndpoint(config));

		if (swaggerEnabled) {
			router.registerPublic("/api/docs", new SwaggerEndpoint());
			router.registerPublic("/api/openapi.json", new OpenApiEndpoint(config.getPort()));
		}
	}

	public void start() {
		httpServer.start();
		MCRestAPI.LOGGER.info("[MCRestAPI] HTTP server started on {}:{}",
				httpServer.getAddress().getHostString(), httpServer.getAddress().getPort());
		MCRestAPI.LOGGER.info("[MCRestAPI] Admin dashboard at http://{}:{}/admin",
				httpServer.getAddress().getHostString(), httpServer.getAddress().getPort());
		if (swaggerEnabled) {
			MCRestAPI.LOGGER.info("[MCRestAPI] Swagger UI at http://{}:{}/api/docs",
					httpServer.getAddress().getHostString(), httpServer.getAddress().getPort());
		}
		logSecurityWarnings();
	}

	private void logSecurityWarnings() {
		if (config.isAuthEnabled()) {
			return;
		}

		String bind = config.getBindAddress();
		MCRestAPI.LOGGER.warn("[MCRestAPI] ============================================================");
		MCRestAPI.LOGGER.warn("[MCRestAPI] AUTHENTICATION IS DISABLED (auth.enabled = false)");
		MCRestAPI.LOGGER.warn("[MCRestAPI] All endpoints, including admin/key management, are OPEN.");
		if (isLoopback(bind)) {
			MCRestAPI.LOGGER.warn("[MCRestAPI] Bound to {} (loopback). Make sure your reverse proxy", bind);
			MCRestAPI.LOGGER.warn("[MCRestAPI] enforces authentication (basic auth, OIDC, etc.).");
		} else {
			MCRestAPI.LOGGER.warn("[MCRestAPI] Bind address is {} (NOT loopback): the API is exposed", bind);
			MCRestAPI.LOGGER.warn("[MCRestAPI] WITHOUT authentication. Ensure a reverse proxy or firewall");
			MCRestAPI.LOGGER.warn("[MCRestAPI] restricts access, or anyone who reaches this port has full control.");
		}
		MCRestAPI.LOGGER.warn("[MCRestAPI] ============================================================");
	}

	private static boolean isLoopback(String addr) {
		return "127.0.0.1".equals(addr) || "::1".equals(addr) || "localhost".equalsIgnoreCase(addr);
	}

	public void stop() {
		httpServer.stop(1);
		MCRestAPI.LOGGER.info("[MCRestAPI] HTTP server stopped");
	}
}
