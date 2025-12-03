#!/bin/bash
# Run in Distributed Mode (All-in-One)

echo "========================================"
echo "Running in DISTRIBUTED MODE (All-in-One)"
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

echo "Starting simulation in distributed mode..."
java -cp "out/production/FinalProject:lib/gson-2.10.1.jar" Main --distributed 1099

