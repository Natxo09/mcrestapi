package net.natxo.mcrestapi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.natxo.mcrestapi.MCRestAPI;
import net.natxo.mcrestapi.security.ApiKey;
import net.natxo.mcrestapi.security.KeyUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ApiConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private transient Path configFile;
	private transient final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private int port = 8080;
	private String bindAddress = "127.0.0.1";
	private int maxConnections = 50;
	private boolean swagger = true;
	private String masterKeyHash = "";
	private AuthConfig auth = new AuthConfig();
	private CorsConfig cors = new CorsConfig();
	private List<ApiKey> keys = new ArrayList<>();

	public static ApiConfig loadOrCreate(Path configDir) {
		Path file = configDir.resolve("mcrestapi.json");

		if (Files.exists(file)) {
			return load(file);
		}

		ApiConfig config = new ApiConfig();
		config.configFile = file;
		config.initializeFirstRun();
		config.save();
		return config;
	}

	private static ApiConfig load(Path file) {
		try {
			String json = Files.readString(file);

			if (isLegacyFormat(json)) {
				MCRestAPI.LOGGER.warn("[MCRestAPI] Detected legacy config format, migrating...");
				return migrateLegacy(json, file);
			}

			ApiConfig config = GSON.fromJson(json, ApiConfig.class);
			config.configFile = file;

			if (config.keys == null) {
				config.keys = new ArrayList<>();
			}
			if (config.cors == null) {
				config.cors = new CorsConfig();
			}
			if (config.auth == null) {
				config.auth = new AuthConfig();
			}

			MCRestAPI.LOGGER.info("[MCRestAPI] Config loaded from {}", file);
			return config;
		} catch (IOException e) {
			MCRestAPI.LOGGER.error("[MCRestAPI] Failed to read config, creating new one", e);
			ApiConfig config = new ApiConfig();
			config.configFile = file;
			config.initializeFirstRun();
			config.save();
			return config;
		}
	}

	private static boolean isLegacyFormat(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		return obj.has("apiKey");
	}

	private static ApiConfig migrateLegacy(String json, Path file) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

		ApiConfig config = new ApiConfig();
		config.configFile = file;

		if (obj.has("port")) config.port = obj.get("port").getAsInt();
		if (obj.has("bindAddress")) config.bindAddress = obj.get("bindAddress").getAsString();
		if (obj.has("maxConnections")) config.maxConnections = obj.get("maxConnections").getAsInt();
		if (obj.has("swagger")) config.swagger = obj.get("swagger").getAsBoolean();

		String legacyKey = obj.get("apiKey").getAsString();
		if (legacyKey != null && !legacyKey.isBlank()) {
			ApiKey migratedKey = new ApiKey(
					KeyUtil.generateId(),
					"Migrated Key",
					KeyUtil.hash(legacyKey),
					List.of("*")
			);
			config.keys.add(migratedKey);
			MCRestAPI.LOGGER.info("[MCRestAPI] Migrated existing API key (unchanged, still works)");
		}

		String masterKey = KeyUtil.generateKey();
		config.masterKeyHash = KeyUtil.hash(masterKey);

		MCRestAPI.LOGGER.info("[MCRestAPI] ========================================");
		MCRestAPI.LOGGER.info("[MCRestAPI] Master key generated (save it now!):");
		MCRestAPI.LOGGER.info("[MCRestAPI] {}", masterKey);
		MCRestAPI.LOGGER.info("[MCRestAPI] ========================================");

		config.save();
		MCRestAPI.LOGGER.info("[MCRestAPI] Config migrated to new format at {}", file);
		return config;
	}

	private void initializeFirstRun() {
		String masterKey = KeyUtil.generateKey();
		masterKeyHash = KeyUtil.hash(masterKey);

		String defaultKey = KeyUtil.generateKey();
		keys.add(new ApiKey(
				KeyUtil.generateId(),
				"Default Key",
				KeyUtil.hash(defaultKey),
				List.of("*")
		));

		MCRestAPI.LOGGER.info("[MCRestAPI] ========================================");
		MCRestAPI.LOGGER.info("[MCRestAPI] First run! Save these keys:");
		MCRestAPI.LOGGER.info("[MCRestAPI] Master key: {}", masterKey);
		MCRestAPI.LOGGER.info("[MCRestAPI] API key:    {}", defaultKey);
		MCRestAPI.LOGGER.info("[MCRestAPI] ========================================");
	}

	public void save() {
		lock.writeLock().lock();
		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(this));
		} catch (IOException e) {
			MCRestAPI.LOGGER.error("[MCRestAPI] Failed to save config", e);
		} finally {
			lock.writeLock().unlock();
		}
	}

	// --- Key management ---

	public ApiKey findKeyByRawValue(String rawKey) {
		lock.readLock().lock();
		try {
			for (ApiKey key : keys) {
				if (KeyUtil.matches(rawKey, key.getHash())) {
					return key;
				}
			}
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}

	public boolean isMasterKey(String rawKey) {
		lock.readLock().lock();
		try {
			return KeyUtil.matches(rawKey, masterKeyHash);
		} finally {
			lock.readLock().unlock();
		}
	}

	public void addKey(ApiKey key) {
		lock.writeLock().lock();
		try {
			keys.add(key);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public boolean removeKey(String id) {
		lock.writeLock().lock();
		try {
			return keys.removeIf(k -> k.getId().equals(id));
		} finally {
			lock.writeLock().unlock();
		}
	}

	public ApiKey findKeyById(String id) {
		lock.readLock().lock();
		try {
			for (ApiKey key : keys) {
				if (key.getId().equals(id)) {
					return key;
				}
			}
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}

	public List<ApiKey> getKeys() {
		lock.readLock().lock();
		try {
			return List.copyOf(keys);
		} finally {
			lock.readLock().unlock();
		}
	}

	// --- Auth ---

	public AuthConfig getAuth() {
		return auth;
	}

	public boolean isAuthEnabled() {
		return auth.isEnabled();
	}

	// --- CORS ---

	public CorsConfig getCors() {
		return cors;
	}

	// --- Getters / Setters ---

	public int getPort() {
		return port;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public boolean isSwaggerEnabled() {
		return swagger;
	}

	public void setSwaggerEnabled(boolean swagger) {
		this.swagger = swagger;
	}

	// --- Auth config inner class ---

	public static class AuthConfig {
		// volatile: the flag is read by request-handling virtual threads and may be
		// flipped live from the admin settings endpoint on a different thread.
		private volatile boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	// --- CORS config inner class ---

	public static class CorsConfig {
		private boolean enabled = false;
		private List<String> allowedOrigins = new ArrayList<>();

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void addOrigin(String origin) {
			if (!allowedOrigins.contains(origin)) {
				allowedOrigins.add(origin);
			}
		}

		public boolean removeOrigin(String origin) {
			return allowedOrigins.remove(origin);
		}
	}
}
