package net.natxo.mcrestapi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.natxo.mcrestapi.MCRestAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;

public class ApiConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private int port = 8080;
	private String bindAddress = "127.0.0.1";
	private int maxConnections = 50;
	private String apiKey = "";
	private boolean swagger = true;

	public static ApiConfig loadOrCreate(Path configDir) {
		Path configFile = configDir.resolve("mcrestapi.json");

		if (Files.exists(configFile)) {
			return load(configFile);
		}

		ApiConfig config = new ApiConfig();
		config.apiKey = generateApiKey();
		config.save(configFile);

		MCRestAPI.LOGGER.info("[MCRestAPI] Config created at {}", configFile);
		MCRestAPI.LOGGER.info("[MCRestAPI] Your API key: {}", config.apiKey);

		return config;
	}

	private static ApiConfig load(Path configFile) {
		try {
			String json = Files.readString(configFile);
			ApiConfig config = GSON.fromJson(json, ApiConfig.class);

			if (config.apiKey == null || config.apiKey.isBlank()) {
				config.apiKey = generateApiKey();
				config.save(configFile);
				MCRestAPI.LOGGER.warn("[MCRestAPI] API key was empty, generated new one: {}", config.apiKey);
			}

			MCRestAPI.LOGGER.info("[MCRestAPI] Config loaded from {}", configFile);
			return config;
		} catch (IOException e) {
			MCRestAPI.LOGGER.error("[MCRestAPI] Failed to read config, using defaults", e);
			ApiConfig config = new ApiConfig();
			config.apiKey = generateApiKey();
			return config;
		}
	}

	private void save(Path configFile) {
		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(this));
		} catch (IOException e) {
			MCRestAPI.LOGGER.error("[MCRestAPI] Failed to save config", e);
		}
	}

	private static String generateApiKey() {
		byte[] bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return "mcsapi_" + HexFormat.of().formatHex(bytes);
	}

	public int getPort() {
		return port;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public String getApiKey() {
		return apiKey;
	}

	public boolean isSwaggerEnabled() {
		return swagger;
	}
}
