@echo off
echo =============================================
echo   Password Manager Demo Setup
echo =============================================
echo.

:: ===========================
:: Step 1: Try to find MySQL
:: ===========================
set FOUND_MYSQL=

:: Common install paths
set MYSQL_PATHS="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe" "C:\xampp\mysql\bin\mysql.exe" "C:\wamp64\bin\mysql\mysql8.0.28\bin\mysql.exe"

for %%p in (%MYSQL_PATHS%) do (
    if exist %%p (
        set FOUND_MYSQL=%%p
        goto mysqlfound
    )
)

:: If not found, ask user to provide full path
echo MySQL executable not found in common locations.
set /p FOUND_MYSQL="Please enter full path to mysql.exe (e.g., C:\xampp\mysql\bin\mysql.exe): "

:mysqlfound
if not exist "%FOUND_MYSQL%" (
    echo MySQL executable not found. Setup cannot continue.
    pause
    exit /b
)

:: ===========================
:: Step 2: Ask for DB credentials
:: ===========================
set /p DB_USER="Enter MySQL username (default: root): "
if "%DB_USER%"=="" set DB_USER=root
set /p DB_PASS="Enter MySQL password (can be empty): "

:: ===========================
:: Step 3: Run SQL setup
:: ===========================
echo Running database setup...
"%FOUND_MYSQL%" -u %DB_USER% -p%DB_PASS% < setup.sql

IF %ERRORLEVEL% NEQ 0 (
    echo There was an error setting up the database.
    pause
    exit /b
)

:: ===========================
:: Step 4: Update config.properties
:: ===========================
echo Writing config.properties...
(
echo db.url=jdbc:mysql://localhost:3306/password_manager
echo db.user=%DB_USER%
echo db.password=%DB_PASS%
) > config.properties

echo.
echo Database setup complete!
echo You can now run PasswordManager.jar or PasswordManager.exe
pause
