# Ditto Edge Server Quickstart

This guide provides step-by-step instructions for getting started with the Ditto Edge Server, a powerful edge computing solution that enables real-time data synchronization and local data processing.

## Prerequisites

- **Just**: Command runner tool ([installation guide](https://github.com/casey/just#installation))
- **curl**: For downloading binaries and making HTTP requests
- **jq**: For JSON processing (used in API examples)
- **Docker** (optional): For running the containerized version

## Supported Platforms

The following platforms are supported by edge-server

- aarch64-unknown-linux-gnu (binary)
- x86_64-unknown-linux-gnu (binary)
- linux/arm64 (docker image)
- linux/amd64 (docker image)

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
- Navigate to your application and obtain the Database ID, Playground Token, and Auth URL (see [Sync Credentials](https://docs.ditto.live/get-started/sync-credentials)
 for more details)

Copy the `quickstart_config_sample.yaml` file at the top level of the quickstart repo to `quickstart_config.yaml` and substitute the appropriate fields in the config with values obtained from the portal.

### Download and Run Edge Server
#### Option 1: Binary Installation

1. **Download the Edge Server binary** for your platform:

   ```bash
   # For Linux x86_64
   just download-binary x86_64-unknown-linux-gnu
   
   # For Linux x86_64
   just download-binary aarch64-unknown-linux-gnu
   ```

2. **Run the Edge Server** with the quickstart configuration:

   ```bash
   just run-quickstart
   ```


#### Option 2: Docker Installation

1. **Download the Edge Server Docker image**:

   ```bash
   # For linux/amd64 platform
   just download-image x86_64-unknown-linux-gnu
   
   # For linux/arm64 platform
   just download-image aarch64-unknown-linux-gnu
   ```

2. **Load the image into Docker**:

   ```bash
   just load-image
   ```

3. **Run the Edge Server container**:

   ```bash
   just run-quickstart-image
   ```

## Testing the API

Once the server is running, you can interact with the Tasks API using the provided commands:

### Create a Task

```bash
# Create a new task with a custom title
just create-task "Buy groceries"
```

### Get All Tasks

```bash
# Retrieve all non-deleted tasks
just get-tasks
```

### Update a Task

```bash
# Mark a task as completed (use the task ID from get-tasks)
just update-task <task-id> done

# Mark a task as not completed
just update-task <task-id> not-done
```

### Delete a Task

```bash
# Soft delete a task (use the task ID from get-tasks)
just delete-task <task-id>
```

## Configuration

The Edge Server uses `quickstart_config.yaml` for configuration. Key settings include:

- **Ditto Database**: Configured with app ID and device name
- **Authentication**: Uses playground provider (development only)
- **HTTP Server**: Listens on port 8080 with `/my_server` base path
- **Subscription**: Monitors all tasks in the database

### Important Security Note

⚠️ **Development Only**: This quickstart uses playground authentication which is NOT suitable for production. For production deployments, configure proper authentication using "Online with Authentication" identity.

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
