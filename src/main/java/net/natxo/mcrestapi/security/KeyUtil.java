package net.natxo.mcrestapi.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class KeyUtil {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String KEY_PREFIX = "mcsapi_";
	private static final String ID_PREFIX = "dk_";
	private static final int KEY_BYTES = 24;
	private static final int ID_BYTES = 4;

	private KeyUtil() {
	}

	public static String generateKey() {
		byte[] bytes = new byte[KEY_BYTES];
		RANDOM.nextBytes(bytes);
		return KEY_PREFIX + HexFormat.of().formatHex(bytes);
	}

	public static String generateId() {
		byte[] bytes = new byte[ID_BYTES];
		RANDOM.nextBytes(bytes);
		return ID_PREFIX + HexFormat.of().formatHex(bytes);
	}

	public static String hash(String key) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}

	public static boolean matches(String rawKey, String storedHash) {
		String keyHash = hash(rawKey);
		return MessageDigest.isEqual(
				keyHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
				storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8)
		);
	}
}
