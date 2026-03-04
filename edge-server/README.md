# Ditto Edge Server Quickstart

This guide provides step-by-step instructions for getting started with the Ditto Edge Server, a powerful edge computing solution that enables real-time data synchronization and local data processing.

## Prerequisites

- **curl**: For downloading binaries and making HTTP requests
- **jq**: For JSON processing (used in API examples)
- **Docker**: For running the containerized version
- The image store for your docker installation must be `containerd` ([setup guide](https://docs.docker.com/desktop/features/containerd/#enable-the-containerd-image-store))
- **just**: Command runner tool ([installation guide](https://github.com/casey/just#installation), optional)

If you prefer to run commands directly, copy-paste commands from the `justfile`.

## Supported Platforms

The following platforms are supported by edge-server

- linux/arm64 (docker image)
- linux/amd64 (docker image)
- aarch64-unknown-linux-gnu (binary)
- x86_64-unknown-linux-gnu (binary)

## Getting Started

### 1. Clone the Repository

- Clone the repository from GitHub. Open a terminal and run the following command:

```bash
git clone https://github.com/getditto/quickstart
```

- Navigate to the project directory:

```bash
cd quickstart/edge-server
```

### 2. Configure Ditto
> [!NOTE] 
>If you haven't created a Ditto Portal account, you can sign up at [Ditto Portal](https://portal.ditto.live) or checkout [this video](https://www.youtube.com/watch?v=1aLiDkgl0Dc) on how to setup a Ditto Portal account.
>

#### Obtain App ID and Playground Token

- Log in to your Ditto Portal account
- Navigate to your application and obtain the Database ID, Playground Token, and Auth URL (see [Sync Credentials](https://docs.ditto.live/cloud/portal/getting-sdk-connection-details)
 for more details)

Copy the `quickstart_config_sample.yaml` file at the top level of the quickstart repo to `quickstart_config.yaml` and substitute the appropriate fields in the config with values obtained from the portal.
```bash
cp quickstart_config_sample.yaml quickstart_config.yaml
```

### Download and Run Edge Server

1. **Download the Edge Server Docker image**:

   ```bash
   # For linux/amd64 platform
   just download x86_64-unknown-linux-gnu
   
   # For linux/arm64 platform
   just download aarch64-unknown-linux-gnu
   ```

2. **Load the image into Docker**:

   ```bash
   just load
   ```

3. **Run the Edge Server container**:

   ```bash
   just run
   ```

## Testing the API

Once the server is running, you can interact with the Tasks API using the provided commands:

### Create a Task

```bash
# Create a new task with a custom title
just create-task Buy groceries
```

### Get All Tasks

```bash
# Retrieve all non-deleted tasks
just get-tasks
```

### Update a Task

```bash
# Mark a task as completed (use the task ID from get-tasks)
just update-task <task-id> true

# Mark a task as not completed
just update-task <task-id> false
```

### Delete a Task

```bash
# Soft delete a task (use the task ID from get-tasks)
just delete-task <task-id>
```

## Additional HTTP API Endpoints

The Edge Server provides additional endpoints for advanced operations:

### Execute DQL Queries

```bash
# Execute custom DQL statements
curl -X POST http://localhost:8080/my_server/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * FROM tasks"}'
```

### Attachments

```bash
# Upload an attachment
curl -X POST http://localhost:8080/my_server/attachments/upload \
  -F "file=@/path/to/file"

# Download an attachment
curl http://localhost:8080/my_server/attachments/{attachment-id} \
  -o downloaded-file
```

### Monitoring & Diagnostics

```bash
# View presence graph
curl http://localhost:8080/my_server/presence

# Download logs (returns .tar.gz)
curl http://localhost:8080/my_server/logs -o edge-server-logs.tar.gz

# View API documentation
curl http://localhost:8080/my_server/docs
```

## API Reference

### OpenAPI Specification

The Edge Server automatically generates an OpenAPI 3.1 specification that documents all available HTTP endpoints. You can access it in two ways:

**Option 1: Runtime Access**
```bash
# View the OpenAPI spec while server is running
curl http://localhost:8080/my_server/docs/edge_server_api.json | jq .
```

**Option 2: Generate Offline**
```bash
# Generate the spec without starting the server
./edge-server config open-api --config quickstart_config.yaml

# Save to a file
./edge-server config open-api --config quickstart_config.yaml --output openapi.json
```

The OpenAPI spec can be used with tools like:
- **Swagger UI** - Interactive API documentation
- **Postman** - Import the spec to auto-generate requests
- **openapi-generator** - Generate client SDKs in various languages
- **Redoc** - Generate static API documentation

## Configuration

The Edge Server uses `quickstart_config.yaml` for configuration. Key settings include:

- **Ditto Database**: Configured with app ID and device name
- **Authentication**: Uses playground provider (development only)
- **HTTP Server**: Listens on port 8080 with `/my_server` base path
- **Subscription**: Monitors all tasks in the database

### Configuration Schema

You can view the full configuration schema to understand all available options:

```bash
# View schema in JSON format
./edge-server config schema json

# View schema in YAML format
./edge-server config schema yaml

# Save to a file for IDE autocomplete
./edge-server config schema json > edge-server-config-schema.json
```

**Tip**: Many IDEs support YAML schema validation. Add this comment at the top of your config file:
```yaml
# yaml-language-server: $schema=./edge-server-config-schema.json
```

### Important Security Note

⚠️ **Development Only**: This quickstart uses playground authentication which is **NOT** secure or suitable for production. For production deployments, configure proper authentication using "Online with Authentication" identity.

### Advanced Security Features

For production deployments, Edge Server supports:

- **HTTPS/TLS**: TLS 1.2 and TLS 1.3 support for encrypted communications
- **API Key Authentication**: Generate and manage API keys for client authentication
- **Permission-Based Access Control**: Configure operation-based permissions for fine-grained access control
- **Audit Logging**: Track and retrieve access logs for security compliance
- **HTTP Request Throttling**: Configure rate limiting on a per-endpoint basis
- **Configurable Limits**: Set TCP connection limits, backlog limits, request timeouts, and keepalive settings

See the [Edge Server documentation](https://docs.ditto.live/edge-server) for configuration details.

## Troubleshooting

### Server won't start
- Ensure port 8080 is not in use: `lsof -i :8080`
- Check the binary has execute permissions: `chmod +x edge-server`

### Cannot download binary
- Verify your platform architecture: `uname -m`
- Check network connectivity to AWS S3
- Ensure curl is installed: `which curl`

### Docker issues
- Verify Docker is running: `docker info`
- Check available disk space for image download

## Next Steps

- Explore other edge server functionaly through the CLI with `just run help`
- Explore the [Ditto documentation](https://docs.ditto.live) for advanced features
- Configure authentication for production use
- Integrate with the other quickstart applications in this repository
- Customize the configuration file for your specific use case

## Files Overview

- `justfile` - Command definitions for common tasks
- `quickstart_config.yaml` - Edge Server configuration
- `requests/` - Example JSON requests for API operations
- `edge-server` - Binary executable (after download)
- `edge-server-image.tar` - Docker image (after download)


## Alternative Option: Binary Installation

If you want to run the edge server as a raw binary for either `aarch64-unknown-linux-gnu` or `x86_64-unknown-linux-gnu` targets, the following `just` commands are provided:

1. **Download the Edge Server binary** for your platform:

   ```bash
   # For Linux x86_64
   just download-bin x86_64-unknown-linux-gnu
   
   # For Linux ARM64
   just download-bin aarch64-unknown-linux-gnu
   ```

2. **Run the Edge Server** with the quickstart configuration:

   ```bash
   just run-bin
