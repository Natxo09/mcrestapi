# MCRestAPI

A Fabric mod for Minecraft 1.21.11 that exposes a REST API and real-time event stream (SSE) for monitoring and controlling dedicated Minecraft servers. Built on top of the JDK's built-in HTTP server with zero external dependencies.

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/XZCgCz7D?label=Modrinth%20Downloads)](https://modrinth.com/mod/mcrestapi)
[![Modrinth Version](https://img.shields.io/modrinth/v/XZCgCz7D?label=Latest%20Version)](https://modrinth.com/mod/mcrestapi/versions)
[![Minecraft Version](https://img.shields.io/modrinth/game-versions/XZCgCz7D?label=Minecraft)](https://modrinth.com/mod/mcrestapi)
[![License: CC0-1.0](https://img.shields.io/badge/License-CC0_1.0-lightgrey.svg)](LICENSE)

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Authentication](#authentication)
  - [API Keys](#api-keys)
  - [Master Key](#master-key)
  - [Permissions](#permissions)
- [API Endpoints](#api-endpoints)
  - [Public Endpoints](#public-endpoints-no-authentication)
  - [Server](#get-apiserver)
  - [Players](#get-apiplayers)
  - [World](#get-apiworld)
  - [Events](#get-apichat)
  - [Command](#post-apicommand)
  - [Admin](#admin-endpoints)
- [CORS Configuration](#cors-configuration)
- [Admin Dashboard](#admin-dashboard)
- [Swagger / OpenAPI](#swagger--openapi)
- [Security](#security)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Features

- REST API for server monitoring (TPS, MSPT, memory, CPU, player count, server properties)
- Real-time Server-Sent Events (SSE) stream for chat, joins, leaves, deaths and game messages
- Complete player data: health, food, position, dimension, ping, gamemode, OP status, skin head URL
- World data: seed, time, weather, difficulty, entity count, loaded chunks, per-dimension stats
- Remote command execution via API
- Built-in admin dashboard (single-page web app) for monitoring and management
- Bundled Swagger UI with OpenAPI 3.1.0 specification
- Multi-key authentication with granular permissions
- Master key for administrative operations
- CORS configuration with per-origin allowlist
- API keys hashed with PBKDF2-SHA256 (never stored in plain text)
- Zero external dependencies (uses JDK built-in HTTP server)
- Virtual threads (Java 21) for lightweight concurrency
- Server-side only (does not run on clients)

---

## Requirements

| Component      | Version         |
|----------------|-----------------|
| Minecraft      | 1.21.11         |
| Fabric Loader  | >= 0.18.4       |
| Fabric API     | any             |
| Java           | >= 21           |

---

## Installation

1. Download the latest `mcrestapi-x.x.x.jar` from [Modrinth](https://modrinth.com/mod/mcrestapi) or [GitHub Releases](https://github.com/Natxo09/mcrestapi/releases)
2. Place the JAR file in the `mods/` folder of your Fabric server
3. Start the server
4. On first launch, the mod generates a config file at `config/mcrestapi.json` and prints the **master key** and **default API key** to the server console. Save both keys immediately — they cannot be retrieved later.

```
[MCRestAPI] ========================================
[MCRestAPI] First run! Save these keys:
[MCRestAPI] Master key: mcsapi_xxxxxxxxxxxxxxxx
[MCRestAPI] API key:    mcsapi_xxxxxxxxxxxxxxxx
[MCRestAPI] ========================================
```

5. The API is available at `http://localhost:8080` by default
6. The admin dashboard is at `http://localhost:8080/admin`

---

## Configuration

The configuration file is located at `config/mcrestapi.json` and is generated automatically on first launch.

### Configuration Fields

| Field            | Type    | Default       | Description                                       |
|------------------|---------|---------------|---------------------------------------------------|
| `port`           | integer | `8080`        | Port the HTTP server listens on                   |
| `bindAddress`    | string  | `"127.0.0.1"` | Address to bind to. Use `0.0.0.0` for all interfaces |
| `maxConnections` | integer | `50`          | Maximum concurrent HTTP connections               |
| `swagger`        | boolean | `true`        | Enable/disable Swagger UI and OpenAPI spec        |
| `masterKeyHash`  | string  | (generated)   | PBKDF2 hash of the master key                     |
| `keys`           | array   | (generated)   | List of API keys with permissions                 |
| `cors`           | object  | (disabled)    | CORS configuration                                |

### Example Configuration

```json
{
  "port": 8080,
  "bindAddress": "127.0.0.1",
  "maxConnections": 50,
  "swagger": true,
  "masterKeyHash": "pbkdf2$...",
  "keys": [
    {
      "id": "dk_a1b2c3d4",
      "name": "Default Key",
      "hash": "pbkdf2$...",
      "permissions": ["*"],
      "createdAt": "2026-03-07T00:00:00Z"
    }
  ],
  "cors": {
    "enabled": false,
    "allowedOrigins": []
  }
}
```

> **Note:** API key hashes are stored using PBKDF2-SHA256. The raw key values are only shown once at creation time and cannot be retrieved from the config file.

---

## Authentication

All API endpoints (except [public endpoints](#public-endpoints-no-authentication)) require authentication via Bearer token in the `Authorization` header.

### API Keys

Include the API key in every request:

```
Authorization: Bearer mcsapi_xxxxxxxxxxxxxxxx
```

Each API key has a name, a set of permissions, and a unique ID. Keys are created via the admin dashboard or the admin API. The raw key value is returned once at creation and cannot be retrieved again.

### Master Key

The master key is a special key generated on first launch that grants access to the admin endpoints (`/api/admin/*`) and the admin dashboard (`/admin`). It also has wildcard permissions for all data endpoints.

The master key is separate from API keys and cannot be managed through the admin interface.

### Permissions

Each API key is assigned a list of permissions that control which endpoints it can access.

| Permission        | Description                  | Endpoints                  |
|-------------------|------------------------------|----------------------------|
| `server.read`     | Read server stats            | `GET /api/server`          |
| `players.read`    | Read player list             | `GET /api/players`         |
| `world.read`      | Read world data              | `GET /api/world`           |
| `chat.read`       | Read event history           | `GET /api/chat`            |
| `chat.stream`     | Connect to SSE event stream  | `GET /api/events/stream`   |
| `command.execute`  | Execute server commands      | `POST /api/command`        |
| `*`               | All permissions (wildcard)   | All endpoints              |

---

## API Endpoints

The base URL is `http://<host>:<port>` (default: `http://localhost:8080`).

All authenticated endpoints return `401 Unauthorized` if no valid key is provided, and `403 Forbidden` if the key lacks the required permission.

### Public Endpoints (no authentication)

| Method | Path               | Description                    |
|--------|--------------------|--------------------------------|
| GET    | `/api/server/icon` | Server icon as PNG image       |
| GET    | `/api/docs`        | Swagger UI (if enabled)        |
| GET    | `/api/openapi.json`| OpenAPI 3.1.0 specification    |

---

### `GET /api/server`

Returns server information and stats. Permission: `server.read`

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `fields`  | No       | Comma-separated list of sections to include: `info`, `tps`, `memory`, `cpu`, `players`, `properties`. Returns all sections if omitted. |

**Example Request:**

```bash
curl -H "Authorization: Bearer mcsapi_xxx" http://localhost:8080/api/server
```

**Example Response:**

```json
{
  "version": "1.21.11",
  "motd": "A Minecraft Server",
  "server_port": 25565,
  "online_mode": true,
  "uptime_seconds": 86400,
  "tps": 20.0,
  "mspt": 12.5,
  "memory": {
    "used_mb": 2048,
    "total_mb": 4096,
    "max_mb": 8192,
    "free_mb": 2048,
    "usage_percent": 50.0
  },
  "cpu": {
    "system_load": 11.47,
    "available_processors": 8,
    "process_load": 15.2
  },
  "players": {
    "online": 5,
    "max": 20
  },
  "properties": {
    "gamemode": "survival",
    "difficulty": "easy",
    "hardcore": false,
    "allow_flight": false,
    "whitelist": false,
    "enforce_whitelist": false,
    "enforce_secure_profile": true,
    "max_world_size": 29999984,
    "spawn_protection": 16,
    "view_distance": 10,
    "simulation_distance": 10,
    "max_tick_time": 60000
  }
}
```

**Filtered Request Example:**

```bash
curl -H "Authorization: Bearer mcsapi_xxx" "http://localhost:8080/api/server?fields=tps,memory"
```

---

### `GET /api/players`

Returns all online players with detailed information. Permission: `players.read`

**Example Request:**

```bash
curl -H "Authorization: Bearer mcsapi_xxx" http://localhost:8080/api/players
```

**Example Response:**

```json
{
  "count": 2,
  "players": [
    {
      "name": "Steve",
      "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
      "health": 20.0,
      "food_level": 18,
      "position": {
        "x": 100.5,
        "y": 64.0,
        "z": -200.3
      },
      "dimension": "minecraft:overworld",
      "ping_ms": 45,
      "game_mode": "survival",
      "is_op": false,
      "skin_head_url": "https://mc-heads.net/avatar/069a79f4-44e9-4726-a5be-fca90e38aaf5/64"
    }
  ]
}
```

---

### `GET /api/world`

Returns global world data and per-dimension stats. Permission: `world.read`

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `fields`  | No       | Comma-separated list of sections: `global`, `dimensions`. Returns all if omitted. |

**Example Request:**

```bash
curl -H "Authorization: Bearer mcsapi_xxx" http://localhost:8080/api/world
```

**Example Response:**

```json
{
  "global": {
    "seed": 123456789,
    "time": {
      "game_time": 72000,
      "day_time": 6000,
      "day_count": 42
    },
    "weather": {
      "raining": false,
      "thundering": false
    },
    "difficulty": "easy",
    "difficulty_locked": false,
    "hardcore": false,
    "pvp": true,
    "spawn": {
      "x": 0,
      "y": 64,
      "z": 0
    }
  },
  "dimensions": [
    {
      "name": "minecraft:overworld",
      "sea_level": 63,
      "flat": false,
      "loaded_chunks": 625,
      "entity_count": 1520,
      "world_border": {
        "size": 59999968.0,
        "center_x": 0.0,
        "center_z": 0.0
      }
    },
    {
      "name": "minecraft:the_nether",
      "sea_level": 63,
      "flat": false,
      "loaded_chunks": 100,
      "entity_count": 340,
      "world_border": {
        "size": 59999968.0,
        "center_x": 0.0,
        "center_z": 0.0
      }
    }
  ]
}
```

---

### `GET /api/chat`

Returns recent server events (chat messages, joins, leaves, deaths, game messages). Permission: `chat.read`

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `limit`   | No       | Maximum number of events to return (default: 50, max: 200) |
| `type`    | No       | Comma-separated event types to filter: `chat`, `command`, `join`, `leave`, `death`, `game` |

**Example Request:**

```bash
curl -H "Authorization: Bearer mcsapi_xxx" "http://localhost:8080/api/chat?limit=10&type=chat,join,leave"
```

**Example Response:**

```json
{
  "count": 3,
  "events": [
    {
      "type": "chat",
      "player": "Steve",
      "content": "Hello everyone!",
      "timestamp": "2026-03-07T22:30:00Z"
    },
    {
      "type": "join",
      "player": "Alex",
      "content": "Alex joined the game",
      "timestamp": "2026-03-07T22:29:00Z"
    },
    {
      "type": "death",
      "player": "Steve",
      "content": "Steve was slain by Zombie",
      "timestamp": "2026-03-07T22:28:00Z"
    }
  ]
}
```

**Event Types:**

| Type      | Description                              |
|-----------|------------------------------------------|
| `chat`    | Player chat messages                     |
| `command` | Command messages (/me, /say)             |
| `join`    | Player joined the server                 |
| `leave`   | Player left the server                   |
| `death`   | Player death messages                    |
| `game`    | Game messages (achievements, system)     |

---

### `GET /api/events/stream`

Real-time event stream via Server-Sent Events (SSE). Permission: `chat.stream`

Connect using any SSE-compatible client. Events are broadcast as they occur on the server. A keepalive ping is sent every 30 seconds to prevent connection timeouts.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `types`   | No       | Comma-separated event types to subscribe to. Receives all types if omitted. |

**SSE Format:**

```
event: chat
data: {"type":"chat","player":"Steve","content":"Hello!","timestamp":"2026-03-07T22:30:00Z"}

event: death
data: {"type":"death","player":"Alex","content":"Alex was slain by Skeleton","timestamp":"2026-03-07T22:31:00Z"}

: keepalive
```

**Example with curl:**

```bash
curl -N -H "Authorization: Bearer mcsapi_xxx" "http://localhost:8080/api/events/stream"
```

**Example with JavaScript:**

```javascript
const eventSource = new EventSource(
  "http://localhost:8080/api/events/stream?auth=mcsapi_xxx"
);

eventSource.addEventListener("chat", (event) => {
  const data = JSON.parse(event.data);
  console.log(`${data.player}: ${data.content}`);
});

eventSource.addEventListener("death", (event) => {
  const data = JSON.parse(event.data);
  console.log(data.content);
});
```

> **Note:** For SSE connections from browsers, you can pass the API key via the `auth` query parameter instead of the `Authorization` header, since the `EventSource` API does not support custom headers.

---

### `POST /api/command`

Executes a Minecraft command on the server. Permission: `command.execute`

The command runs on the server thread with operator-level permissions. Output is captured and returned in the response.

**Request Body:**

```json
{
  "command": "say Hello World"
}
```

> **Note:** Do not include the leading `/` in the command.

**Example Request:**

```bash
curl -X POST \
  -H "Authorization: Bearer mcsapi_xxx" \
  -H "Content-Type: application/json" \
  -d '{"command": "say Hello World"}' \
  http://localhost:8080/api/command
```

**Example Response:**

```json
{
  "success": true,
  "output": ""
}
```

---

### Admin Endpoints

All admin endpoints require the **master key** for authentication. They are used to manage API keys, CORS settings, and server configuration.

#### `GET /api/admin/keys` — List all API keys

Returns all API keys with their ID, name, permissions, and creation date. Key hashes are not included.

#### `POST /api/admin/keys` — Create a new API key

**Request Body:**

```json
{
  "name": "Dashboard",
  "permissions": ["server.read", "players.read"]
}
```

**Response (201):**

```json
{
  "id": "dk_a1b2c3d4",
  "name": "Dashboard",
  "key": "mcsapi_xxxxxxxxxxxxxxxx",
  "permissions": ["server.read", "players.read"],
  "created_at": "2026-03-07T00:00:00Z",
  "warning": "Save this key now. It will not be shown again."
}
```

#### `PUT /api/admin/keys?id=<key_id>` — Update an API key

Update the name or permissions of an existing key.

#### `DELETE /api/admin/keys?id=<key_id>` — Revoke an API key

Permanently deletes the API key.

#### `GET /api/admin/cors` — Get CORS configuration

#### `POST /api/admin/cors` — Add an allowed origin

```json
{ "origin": "https://example.com" }
```

#### `PUT /api/admin/cors` — Toggle CORS enabled/disabled

```json
{ "enabled": true }
```

#### `DELETE /api/admin/cors?origin=<url>` — Remove an allowed origin

#### `GET /api/admin/settings` — Get current settings

Returns port, bind address, max connections, and swagger enabled status.

#### `PUT /api/admin/settings` — Update settings

```json
{ "swagger_enabled": false }
```

---

## CORS Configuration

Cross-Origin Resource Sharing (CORS) can be configured to allow browser-based applications to access the API from different origins.

- CORS is **disabled by default**
- When enabled, only origins in the allowlist receive CORS headers
- Preflight `OPTIONS` requests return `204 No Content` with the appropriate CORS headers
- CORS can be managed through the admin dashboard or the admin API endpoints

**CORS headers returned when enabled:**

```
Access-Control-Allow-Origin: https://your-dashboard.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Max-Age: 3600
```

> **Important:** CORS only provides protection in the browser context. It does not replace API key authentication. Any HTTP client can still access the API with a valid key regardless of CORS settings.

---

## Admin Dashboard

The mod includes a built-in web dashboard for monitoring and managing the server.

**URL:** `http://<host>:<port>/admin` (default: `http://localhost:8080/admin`)

**Authentication:** Requires the master key.

**Available pages:**

- **Dashboard** — Real-time server stats: TPS, MSPT, memory usage bar, CPU load, server properties
- **Players** — Online player list with skin avatars, health/food bars, position, dimension, ping
- **World** — In-game time, weather, difficulty, per-dimension entity count, loaded chunks, world border
- **Chat** — Live chat stream via SSE with player avatars and event type filters
- **Console** — Execute server commands with output display and command history navigation
- **Keys** — Create, update, and revoke API keys
- **CORS** — Manage allowed origins and toggle CORS on/off
- **Settings** — General configuration (toggle Swagger UI)

---

## Swagger / OpenAPI

The mod bundles a Swagger UI instance and an OpenAPI 3.1.0 specification for interactive API exploration.

- **Swagger UI:** `http://<host>:<port>/api/docs`
- **OpenAPI spec:** `http://<host>:<port>/api/openapi.json`

Both endpoints are public (no authentication required). Swagger UI can be disabled by setting `swagger` to `false` in the config file or via the admin settings endpoint.

---

## Security

### Bind Address

By default, the API binds to `127.0.0.1` (localhost only), meaning it is not accessible from other machines on the network. To allow remote access, change `bindAddress` to `0.0.0.0` in the config file.

### Key Storage

API keys are hashed with PBKDF2-SHA256 with a random salt before being stored in the config file. Raw key values are never written to disk. They are only printed once at creation time.

### Master Key

The master key is generated on first launch and its hash is stored separately from regular API keys. It provides access to administrative endpoints and the admin dashboard.

### Recommendations for Production

- Use a reverse proxy (nginx, Caddy) with HTTPS/TLS for remote access
- Keep the default `bindAddress` as `127.0.0.1` and proxy through the reverse proxy
- Create API keys with minimal permissions needed for each use case
- Rotate API keys periodically by revoking and creating new ones
- Enable CORS only if browser-based clients need access, and restrict origins to specific domains

---

## Building from Source

```bash
git clone https://github.com/Natxo09/mcrestapi.git
cd mcrestapi
./gradlew build
```

The compiled mod JAR is located at `build/libs/mcrestapi-<version>.jar`.

To launch a test server with the mod loaded:

```bash
./gradlew runServer
```

On first run, accept the EULA by editing `run/eula.txt`.

---

## License

This project is licensed under [CC0 1.0 Universal](LICENSE) — public domain dedication.
