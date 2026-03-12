# postgres-mcp

A Spring Boot MCP (Model Context Protocol) server that exposes PostgreSQL database tools to AI clients such as Claude Desktop.

## Overview

This server implements the [MCP Streamable HTTP transport](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http), allowing AI models to interact with a PostgreSQL database through a set of read-safe tools.

## Requirements

- Java 17+
- Maven 3.8+
- PostgreSQL database
- An MCP-compatible client (e.g. Claude Desktop, MCP Inspector)

## Getting Started

### 1. Configure the database

Edit `src/main/resources/application.yaml`:

```yaml
server:
  port: 8033

spring:
  application:
    name: postgres-mcp
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: password
    driver-class-name: org.postgresql.Driver
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        name: postgres-mcp
        version: 1.0.0
        type: SYNC
```

### 2. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

You should see the following in the logs confirming the server is ready:

```
Registered tools: 3
Netty started on port 8033 (http)
```

## Available Tools

| Tool | Description |
|---|---|
| `list_tables` | Lists all tables in the `public` schema along with their descriptions |
| `describe_table` | Returns all columns, types, nullability, defaults, and comments for a given table |
| `query` | Executes a read-only `SELECT` SQL query and returns the results |

### `list_tables`

No parameters required. Returns each table's name and its `COMMENT ON TABLE` description if set.

Example response:
```json
[
  { "table_name": "sales", "table_description": "Stores all sales transactions" },
  { "table_name": "users", "table_description": null }
]
```

### `describe_table`

| Parameter | Type | Description |
|---|---|---|
| `tableName` | `string` | Name of the table to describe |

Example response:
```json
[
  {
    "column_name": "id",
    "data_type": "integer",
    "character_maximum_length": null,
    "is_nullable": "NO",
    "column_default": "nextval('sales_id_seq'::regclass)",
    "column_description": "Primary key"
  }
]
```

### `query`

| Parameter | Type | Description |
|---|---|---|
| `sql` | `string` | A `SELECT` SQL query to execute |

Only `SELECT` statements are permitted. Any other statement type will return an error.

Example:
```sql
SELECT * FROM sales WHERE amount > 1000 LIMIT 10
```

## Connecting a Client

### Claude Desktop

Add the following to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "postgres-mcp": {
      "type": "http",
      "url": "http://localhost:8033/mcp"
    }
  }
}
```

### MCP Inspector or other clients

- **Transport Type:** Streamable HTTP
- **URL:** `http://localhost:8033/mcp`
- **Connection Type:** Direct (not Via Proxy)

### Test with curl

```bash
# List tools
curl -X POST http://localhost:8033/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'

# Call list_tables
curl -X POST http://localhost:8033/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"list_tables","arguments":{}},"id":2}'
```

## Project Structure

```
src/
└── main/
    ├── java/com/example/postgres_mcp/
    │   ├── PostgresMcpApplication.java   # Main application class
    │   └── services/
    │       └── postgresService.java      # MCP tool definitions
    └── resources/
        └── application.yaml             # Server and datasource config
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `spring-ai-starter-mcp-server-webflux` | MCP server with Streamable HTTP over Netty |
| `spring-boot-starter-jdbc` | JDBC support and `JdbcTemplate` |
| `postgresql` | PostgreSQL JDBC driver |


<img width="1887" height="872" alt="image" src="https://github.com/user-attachments/assets/dc3527d4-ffd4-4cd3-8c5f-50c3e8ce124c" />


### MCP Inspector

[MCP Inspector](https://github.com/modelcontextprotocol/inspector) is the official browser-based tool for testing and debugging MCP servers. It lets you browse available tools, call them manually, and inspect raw request/response payloads.

#### Install and launch

```bash
npx @modelcontextprotocol/inspector
```

This opens the Inspector UI in your browser at `http://localhost:5173`.

#### Connect to the server

Fill in the connection form as follows:

| Field | Value |
|---|---|
| **Transport Type** | `Streamable HTTP` |
| **URL** | `http://localhost:8033/mcp` |
| **Connection Type** | `Direct` *(not Via Proxy)* |

Then click **Connect**. If the server is running you will see a green connected status and the sidebar will populate with the server name `postgres-mcp`.

#### Browse and call tools

1. Click the **Tools** tab in the left sidebar.
2. You will see all three registered tools: `list_tables`, `describe_table`, and `query`.
3. Select a tool to expand its input form.
4. Fill in any parameters and click **Run Tool**.
5. The response panel on the right shows the raw JSON result.

#### Example: calling `describe_table`

In the `describe_table` input form set:

```
tableName: sales
```

Click **Run Tool**. The response will list every column in the `sales` table with its type, nullability, default value, and description.

#### Troubleshooting

- **Connection Error / proxy token required** — make sure **Connection Type** is set to `Direct`, not `Via Proxy`.
- **No tools listed** — confirm the server log shows `Registered tools: 3` and `Netty started on port 8033`.
- **CORS error in browser** — add the following to `application.yaml` to allow 
## Security Notes

- The `query` tool only permits `SELECT` statements — write operations are blocked at the application level.
- No authentication is configured by default. For production use, consider adding Spring Security or restricting network access to the server port.
- Database credentials are stored in plaintext in `application.yaml` — use environment variables or a secrets manager in production.
