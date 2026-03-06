package net.natxo.mcrestapi.collectors;

import net.minecraft.server.MinecraftServer;

public class TpsCollector {

	private static final int BUFFER_SIZE = 20;

	private final double[] tickTimes = new double[BUFFER_SIZE];
	private int tickIndex = 0;
	private long lastTickNano = 0;
	private volatile double cachedTps = 20.0;
	private volatile double cachedMspt = 0.0;
	private final long startTimeMillis = System.currentTimeMillis();

	public void onEndTick(MinecraftServer server) {
		long now = System.nanoTime();

		if (lastTickNano > 0) {
			double tickMs = (now - lastTickNano) / 1_000_000.0;
			tickTimes[tickIndex % BUFFER_SIZE] = tickMs;
			tickIndex++;

			int count = Math.min(tickIndex, BUFFER_SIZE);
			double sum = 0.0;
			for (int i = 0; i < count; i++) {
				sum += tickTimes[i];
			}
			double avgMs = sum / count;

			cachedMspt = Math.round(avgMs * 100.0) / 100.0;
			cachedTps = Math.round(Math.min(20.0, 1000.0 / avgMs) * 100.0) / 100.0;
		}

		lastTickNano = now;
	}

	public double getTps() {
		return cachedTps;
	}

	public double getMspt() {
		return cachedMspt;
	}

	public long getUptimeSeconds() {
		return (System.currentTimeMillis() - startTimeMillis) / 1000;
	}
}
