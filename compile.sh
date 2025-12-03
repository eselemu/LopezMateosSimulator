#!/bin/bash
# Compile all Java files

echo "Compiling project..."
find src -name "*.java" > sources.txt
javac -d out/production/FinalProject -cp "lib/gson-2.10.1.jar" @sources.txt
rm sources.txt

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
else
    echo "Compilation failed!"
    exit 1
fi

