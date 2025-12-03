@echo off
REM Run in Local Mode (No RMI)
echo ========================================
echo Running in LOCAL MODE
echo ========================================
echo.

REM Compile if needed
if not exist "out\production\FinalProject\Main.class" (
    echo Compiling...
    call compile.bat
    if errorlevel 1 (
        echo Compilation failed!
        pause
        exit /b 1
    )
)

echo Starting simulation in local mode...
java -cp "out/production/FinalProject;lib/gson-2.10.1.jar" Main

pause

