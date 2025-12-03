#!/bin/bash
# Run in Local Mode (No RMI)

echo "========================================"
echo "Running in LOCAL MODE"
echo "========================================"
echo ""

# Compile if needed
if [ ! -f "out/production/FinalProject/Main.class" ]; then
    echo "Compiling..."
    ./compile.sh
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
fi

echo "Starting simulation in local mode..."
java -cp "out/production/FinalProject:lib/gson-2.10.1.jar" Main

