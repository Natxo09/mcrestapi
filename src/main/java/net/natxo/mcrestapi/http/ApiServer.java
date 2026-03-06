package net.natxo.mcrestapi.http;

import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.natxo.mcrestapi.MCRestAPI;
import net.natxo.mcrestapi.collectors.PlayerTracker;
import net.natxo.mcrestapi.collectors.TpsCollector;
import net.natxo.mcrestapi.config.ApiConfig;
import net.natxo.mcrestapi.endpoints.AdminDashboardEndpoint;
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

	public ApiServer(ApiConfig config, TpsCollector tpsCollector, PlayerTracker playerTracker, DedicatedServer server) throws IOException {
		InetSocketAddress address = new InetSocketAddress(config.getBindAddress(), config.getPort());
		this.httpServer = HttpServer.create(address, config.getMaxConnections());
		this.httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		this.swaggerEnabled = config.isSwaggerEnabled();

		Router router = new Router(httpServer, config);
		router.register("/api/server", new ServerEndpoint(tpsCollector, playerTracker, server), "server.read");
		router.register("/api/players", new PlayersEndpoint(playerTracker), "players.read");
		router.register("/api/world", new WorldEndpoint(server), "world.read");

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
	}

	public void stop() {
		httpServer.stop(1);
		MCRestAPI.LOGGER.info("[MCRestAPI] HTTP server stopped");
	}
}
