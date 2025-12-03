@echo off
REM Run in Distributed Mode (All-in-One)
echo ========================================
echo Running in DISTRIBUTED MODE (All-in-One)
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

echo Starting simulation in distributed mode...
java -cp "out/production/FinalProject;lib/gson-2.10.1.jar" Main --distributed 1099

pause

