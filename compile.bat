@echo off
REM Compile all Java files
echo Compiling project...
powershell -Command "javac -d out/production/FinalProject -cp 'lib/gson-2.10.1.jar' (Get-ChildItem -Recurse src -Filter *.java).FullName"
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
) else (
    echo Compilation successful!
)

