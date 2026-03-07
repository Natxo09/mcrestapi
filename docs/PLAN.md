# MCRestAPI — Mod Fabric REST API para Minecraft

## Planificacion Tecnica del Proyecto

---

## 1. Estado Actual del Ecosistema

### Version de Minecraft

- **Ultima version estable de Java Edition:** `1.21.11` — *Mounts of Mayhem*, lanzada el **9 de diciembre de 2025**
- **Proxima version:** `26.1` — *Tiny Takeover* (nuevo sistema de versionado basado en anno). Aun en snapshots, se espera para marzo 2026.
- **Nota importante:** 1.21.11 es la **ultima version ofuscada** de Minecraft. A partir de 26.1, Mojang elimina la ofuscacion del codigo, lo que simplificara enormemente el desarrollo de mods.
- **Java requerido:** Java 21 para 1.21.11 (26.1 requerira Java 25)

### Versiones de Fabric (para 1.21.11)

| Componente       | Version                    |
|------------------|----------------------------|
| Fabric Loom      | `1.15-SNAPSHOT`            |
| Fabric Loader    | `0.18.4`                   |
| Fabric API       | `0.141.3+1.21.11`          |
| Mappings         | Mojang oficiales           |
| Java Target      | `21`                       |

### Por que desarrollar en 1.21.11

- 1.21.11 es la version estable con mayor base de jugadores actualmente
- El port a 26.1 sera mas sencillo gracias a la des-ofuscacion
- Fabric ya trabaja activamente en soporte para 26.1 (hay Fabric API en snapshots)
- Al usar Mojang Mappings, el port a 26.1 sera casi directo

---

## 2. Eventos de Fabric API Disponibles

### Ciclo de vida del servidor

- `ServerLifecycleEvents.SERVER_STARTING` — Antes de cargar mundos, ideal para inicializar el servidor HTTP
- `ServerLifecycleEvents.SERVER_STARTED` — Servidor listo, mundos cargados, jugadores pueden conectarse
- `ServerLifecycleEvents.SERVER_STOPPING` — Inicio del apagado, jugadores aun conectados
- `ServerLifecycleEvents.SERVER_STOPPED` — Todo cerrado, ideal para cleanup del servidor HTTP

### Ticks del servidor (para TPS)

- `ServerTickEvents.START_SERVER_TICK` — Inicio de cada tick
- `ServerTickEvents.END_SERVER_TICK` — Fin de cada tick (medir tiempo entre ticks = TPS)

### Mensajes y Chat

- `ServerMessageEvents.CHAT_MESSAGE` — Chat de jugadores (desde GUI o comando)
- `ServerMessageEvents.COMMAND_MESSAGE` — Mensajes de comandos (/me, /say)
- `ServerMessageEvents.GAME_MESSAGE` — Mensajes del juego (muertes, join/leave, logros)
- `ServerMessageEvents.ALLOW_CHAT_MESSAGE` — Permite bloquear mensajes (moderacion)

### Conexion de jugadores

- `ServerPlayConnectionEvents.JOIN` — Jugador se conecta
- `ServerPlayConnectionEvents.DISCONNECT` — Jugador se desconecta

### Mundos

- `ServerWorldEvents.LOAD` — Mundo cargado
- `ServerWorldEvents.UNLOAD` — Mundo descargado

---

## 3. Servidor HTTP Embebido

### Decision: `com.sun.net.httpserver`

**Razones:**

- **Zero dependencias externas** — no hay conflicto de classpath con Minecraft (que usa Netty internamente)
- **Incluido en el JDK** — no hay que hacer shade ni fat-jar de librerias HTTP
- **NIO** — rendimiento decente sin bloquear hilos
- **Comprobado** — Oracle JDK y OpenJDK lo incluyen; es el estandar de facto para servidores embebidos ligeros

Para WebSocket, se implementa el handshake HTTP Upgrade manualmente o se usa el WebSocket del JDK 21+ (`java.net.http`).

### Alternativas evaluadas y descartadas

| Opcion                          | Pros                                              | Contras                                    | Veredicto     |
|---------------------------------|---------------------------------------------------|--------------------------------------------|---------------|
| `com.sun.net.httpserver`        | Zero deps, incluido en JDK, NIO, ligero          | API algo cruda, sin WebSocket nativo       | **Elegido**   |
| NanoHTTPD                       | Un solo archivo Java, WebSocket incluido, BSD     | Proyecto semi-abandonado                   | Alternativa   |
| Javalin (Jetty)                 | API moderna y elegante, WebSocket integrado       | Dependencia pesada, conflicto con Netty    | No recomendado|

---

## 4. Arquitectura del Mod

```
mcrestapi/
src/main/java/net/natxo/mcrestapi/
    MCRestAPI.java                          // Entrypoint principal (ModInitializer)
    config/
        ApiConfig.java                      // Configuracion: puerto, bind, API keys, CORS
    http/
        ApiServer.java                      // Wrapper del HttpServer del JDK
        Router.java                         // Mapeo de rutas a handlers
        middleware/
            AuthMiddleware.java             // Validacion de API key (Bearer token)
            CorsMiddleware.java             // Headers CORS configurables
            RateLimiter.java                // Rate limiting por IP/key
        websocket/
            WebSocketHandler.java           // Para chat y eventos en tiempo real
    endpoints/
        ServerEndpoint.java                 // GET /api/server -> RAM, TPS, version, uptime
        PlayersEndpoint.java                // GET /api/players -> Lista, salud, posicion
        WorldEndpoint.java                  // GET /api/world -> Info del mundo, clima, tiempo
        ChatEndpoint.java                   // GET /api/chat -> Historial + WS stream
        CommandEndpoint.java                // POST /api/command -> Ejecutar comandos (admin)
    collectors/
        TpsCollector.java                   // Calcula TPS en cada tick
        ChatCollector.java                  // Buffer circular de mensajes recientes
        PlayerTracker.java                  // Tracking de join/leave/posicion
src/main/resources/
    fabric.mod.json                         // Metadatos del mod
    mcrestapi.mixins.json                   // (si se necesitan mixins)
```

---

## 5. Endpoints API

### `GET /api/server`

```json
{
  "name": "Mi Servidor",
  "motd": "Bienvenidos!",
  "version": "1.21.11",
  "online": true,
  "tps": 19.98,
  "mspt": 12.5,
  "uptime_seconds": 86400,
  "memory": {
    "used_mb": 2048,
    "total_mb": 4096,
    "max_mb": 8192,
    "free_mb": 2048,
    "usage_percent": 50.0
  },
  "cpu": {
    "process_load": 15.2,
    "system_load": 30.5,
    "available_processors": 8
  },
  "players": {
    "online": 5,
    "max": 20
  }
}
```

### `GET /api/players`

```json
{
  "count": 2,
  "players": [
    {
      "name": "Steve",
      "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "health": 20.0,
      "food_level": 18,
      "position": { "x": 100.5, "y": 64.0, "z": -200.3 },
      "dimension": "minecraft:overworld",
      "ping_ms": 45,
      "game_mode": "SURVIVAL",
      "is_op": false
    }
  ]
}
```

### `GET /api/world`

```json
{
  "worlds": [
    {
      "name": "minecraft:overworld",
      "seed": 123456789,
      "time": 6000,
      "day_count": 42,
      "weather": {
        "raining": false,
        "thundering": false
      },
      "loaded_chunks": 625,
      "entity_count": 1520,
      "difficulty": "HARD",
      "spawn": { "x": 0, "y": 64, "z": 0 }
    }
  ]
}
```

### `GET /api/chat?limit=50`

```json
{
  "messages": [
    {
      "timestamp": "2025-12-10T14:30:00Z",
      "type": "CHAT",
      "player": "Steve",
      "content": "Hola a todos!"
    },
    {
      "timestamp": "2025-12-10T14:30:05Z",
      "type": "GAME",
      "player": null,
      "content": "Alex joined the game"
    }
  ]
}
```

### `WS /api/chat/stream` (WebSocket)

Eventos en tiempo real:

```json
{"event": "chat", "player": "Steve", "content": "gg", "timestamp": "..."}
{"event": "join", "player": "Alex", "timestamp": "..."}
{"event": "leave", "player": "Notch", "timestamp": "..."}
{"event": "death", "player": "Steve", "message": "Steve was slain by Zombie", "timestamp": "..."}
{"event": "advancement", "player": "Alex", "advancement": "minecraft:story/iron_tools", "timestamp": "..."}
```

### `POST /api/command` (requiere permisos admin)

Request:

```json
{ "command": "say Mantenimiento en 5 minutos" }
```

Response:

```json
{ "success": true, "output": "Comando ejecutado" }
```

---

## 6. Sistema de Seguridad

### 6.1 Archivo de configuracion

El mod genera un archivo de configuracion al primer inicio:

```json
{
  "server": {
    "port": 8080,
    "bind_address": "127.0.0.1",
    "max_connections": 50
  },
  "security": {
    "api_keys": [
      {
        "key": "mcsapi_a1b2c3d4e5f6...",
        "name": "Dashboard Principal",
        "permissions": ["server.read", "players.read", "world.read", "chat.read", "chat.stream"],
        "created_at": "2025-12-10T00:00:00Z"
      },
      {
        "key": "mcsapi_x9y8z7w6v5u4...",
        "name": "Admin Bot",
        "permissions": ["*"],
        "created_at": "2025-12-10T00:00:00Z"
      }
    ],
    "rate_limit": {
      "requests_per_minute": 60,
      "burst": 10
    }
  },
  "cors": {
    "enabled": true,
    "allowed_origins": ["http://localhost:3000"],
    "allowed_methods": ["GET", "POST", "OPTIONS"],
    "allowed_headers": ["Authorization", "Content-Type"],
    "max_age": 3600
  }
}
```

### 6.2 Autenticacion

Cada peticion requiere el header:

```
Authorization: Bearer mcsapi_a1b2c3d4e5f6...
```

### 6.3 CORS

Headers HTTP que el servidor devuelve:

```
Access-Control-Allow-Origin: https://mi-dashboard.com
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Max-Age: 3600
```

Para peticiones preflight (OPTIONS), se responde con status `204 No Content` y los headers CORS.

**Importante:** CORS solo protege en el contexto del navegador. Es complementario a la API key, no un sustituto.

### 6.4 Bind Address

- `127.0.0.1` (default) — Solo accesible desde la misma maquina. Seguro para desarrollo.
- `0.0.0.0` — Accesible desde cualquier IP. Solo usar si el admin lo configura explicitamente.

### 6.5 Rate Limiting

Token bucket por IP y por API key. Configurable: requests por minuto y burst maximo.

### 6.6 Permisos granulares

| Permiso          | Descripcion                     | Endpoints                     |
|------------------|---------------------------------|-------------------------------|
| `server.read`    | Info del servidor               | `GET /api/server`             |
| `players.read`   | Lista de jugadores              | `GET /api/players`            |
| `world.read`     | Info del mundo                  | `GET /api/world`              |
| `chat.read`      | Historial de chat               | `GET /api/chat`               |
| `chat.stream`    | WebSocket del chat              | `WS /api/chat/stream`         |
| `command.execute` | Ejecutar comandos              | `POST /api/command`           |
| `*`              | Todos los permisos              | Todos                         |

---

## 7. Consideraciones Tecnicas Criticas

### Thread Safety

Todo acceso al estado del servidor de Minecraft debe ejecutarse en el hilo del servidor:

```java
// MAL -- race condition
String playerName = server.getPlayerManager().getPlayerList().get(0).getName().getString();

// BIEN -- ejecutar en el hilo del servidor
server.execute(() -> {
    String playerName = server.getPlayerManager().getPlayerList().get(0).getName().getString();
    // Serializar y enviar respuesta
});
```

Para lecturas frecuentes (TPS, RAM), se pueden cachear los valores en cada tick del servidor y servirlos sin cambiar de hilo.

### Calculo de TPS

```java
// En ServerTickEvents.END_SERVER_TICK:
long now = System.nanoTime();
if (lastTickTime > 0) {
    double tickMs = (now - lastTickTime) / 1_000_000.0;
    tickTimes[tickIndex++ % 20] = tickMs;  // Buffer circular de 20 ticks
}
lastTickTime = now;

// TPS = min(20, 1000 / avgTickMs)
double avgMs = Arrays.stream(tickTimes).average().orElse(50.0);
double tps = Math.min(20.0, 1000.0 / avgMs);
```

### Memoria

```java
Runtime rt = Runtime.getRuntime();
long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
long maxMb = rt.maxMemory() / (1024 * 1024);
long totalMb = rt.totalMemory() / (1024 * 1024);
```

### CPU (via MXBean)

```java
OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
double systemLoad = osBean.getSystemLoadAverage();
// Para process CPU, cast a com.sun.management.OperatingSystemMXBean
```

---

## 8. Roadmap de Desarrollo

### Fase 1 — MVP

- [ ] Scaffold del proyecto con Fabric template
- [ ] Servidor HTTP embebido con `com.sun.net.httpserver`
- [ ] Endpoint `/api/server` (RAM, TPS, version)
- [ ] Endpoint `/api/players` (lista basica)
- [ ] Sistema de API key simple
- [ ] Archivo de configuracion JSON

### Fase 2 — Core Features

- [ ] Endpoint `/api/world`
- [ ] Endpoint `/api/chat` (historial)
- [ ] CORS configurable
- [ ] Rate limiting
- [ ] Permisos granulares por API key

### Fase 3 — Real-time

- [ ] WebSocket para chat en tiempo real
- [ ] Eventos de join/leave/death/advancement via WS
- [ ] Endpoint `/api/command` con permisos admin

### Fase 4 — Polish

- [ ] Comando in-game `/mcserverapi` para gestionar keys
- [x] Documentacion de la API (OpenAPI/Swagger)
- [ ] Tests
- [x] Publicacion en Modrinth (Minotaur + CI/CD)
- [ ] Publicacion en CurseForge
- [ ] Port a 26.1 cuando se estabilice

---

## 9. Consideracion sobre la version 26.1

Fabric ha anunciado que con la des-ofuscacion de Minecraft:

- **Fabric Loader y Loom podran soportar nuevas versiones inmediatamente** al salir, sin necesidad de actualizar Yarn o Intermediary manualmente.
- Se recomienda usar **Mojang Mappings** (ya configurado en este proyecto) para facilitar la transicion.
- Los mods deberan **recompilarse** para 26.1 (no son compatibles binariamente), pero la migracion de codigo sera minima.
