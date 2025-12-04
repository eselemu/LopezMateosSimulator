# Lopez Mateos Traffic Simulator - Distributed Execution

## Quick Start

### Compile Project
```batch
# Windows
compile.bat

# Linux/macOS
chmod +x compile.sh
./compile.sh
```

### Single Machine (All-in-One)
```batch
# Windows
run-distributed.bat

# Linux/macOS
chmod +x run-distributed.sh
./run-distributed.sh
```

### Multi-Terminal/Multi-Machine Testing

**Terminal 1 (Server - Semaphores Only):**
```batch
# Windows
start-server.bat

# Linux/macOS
chmod +x start-server.sh
./start-server.sh
```

**Terminal 2 (Client - Agents Only):**
```batch
# Windows
start-client.bat
# Enter server IP (or press Enter for localhost)
# Enter port (or press Enter for 1099)

# Linux/macOS
chmod +x start-client.sh
./start-client.sh
# Enter server IP (or press Enter for localhost)
# Enter port (or press Enter for 1099)
```

## Manual Execution (If Scripts Don't Work)

### Compile
```batch
# Windows (PowerShell)
javac -d out/production/FinalProject -cp "lib/gson-2.10.1.jar" (gci -r src -filter *.java).FullName

# Linux/macOS
find src -name "*.java" > sources.txt
javac -d out/production/FinalProject -cp "lib/gson-2.10.1.jar" @sources.txt
rm sources.txt
```

### Run Server
```batch
# Windows
java -cp "out/production/FinalProject;lib/gson-2.10.1.jar" Main --server

# Linux/macOS
java -cp "out/production/FinalProject:lib/gson-2.10.1.jar" Main --server
```

### Run Client
```batch
# Windows
java -cp "out/production/FinalProject;lib/gson-2.10.1.jar" Main --client localhost 1099

# Linux/macOS
java -cp "out/production/FinalProject:lib/gson-2.10.1.jar" Main --client localhost 1099
```

## Testing Across Different Computers

1. **On Computer 1 (Server):**
   - Run `start-server.bat` or `start-server.sh`
   - Note the IP address of this computer

2. **On Computer 2 (Client):**
   - Run `start-client.bat` or `start-client.sh`
   - When prompted, enter Computer 1's IP address
   - Enter port (default: 1099)

## Requirements

- JDK 17 or higher
- Network connectivity (for multi-machine setup)
- Firewall allows port 1099 (for multi-machine setup)

## Files

- `compile.bat/sh` - Compile all Java files
- `start-server.bat/sh` - Run server (semaphores only)
- `start-client.bat/sh` - Run client (agents only)
- `run-distributed.bat/sh` - Run everything on one machine
- `run-local.bat/sh` - Run in local mode (no RMI)
