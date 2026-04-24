@echo off
echo ================================
echo    ZealStyx Build and Package
echo ================================
echo.

echo [1/4] Building project...
call gradlew.bat :lwjgl3:dist
if %errorlevel% neq 0 (
    echo BUILD FAILED. Check errors above.
    pause
    exit /b 1
)
echo Build successful.
echo.

set /p APP_NAME="[2/4] Enter app name (default: ZealStyx): "
if "%APP_NAME%"=="" set APP_NAME=ZealStyx

set /p APP_VERSION="[3/4] Enter version (default: 1.0): "
if "%APP_VERSION%"=="" set APP_VERSION=1.0

set /p PKG_TYPE="[4/4] Package type - exe or msi (default: exe): "
if "%PKG_TYPE%"=="" set PKG_TYPE=exe
if /i "%PKG_TYPE%" neq "exe" if /i "%PKG_TYPE%" neq "msi" (
    echo Invalid type. Using exe.
    set PKG_TYPE=exe
)

echo.
echo Packaging %APP_NAME% v%APP_VERSION% as .%PKG_TYPE%...
echo.

"C:\Program Files\Java\jdk-17\bin\jpackage.exe" ^
  --input lwjgl3/build/libs ^
  --main-jar Resonance-1.0.0.jar ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --type %PKG_TYPE% ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --icon lwjgl3\icons\logo.ico

if %errorlevel% neq 0 (
    echo PACKAGING FAILED. Check errors above.
    pause
    exit /b 1
)

echo.
echo ================================
echo  Done! Installer created:
echo  %APP_NAME%-%APP_VERSION%.%PKG_TYPE%
echo ================================
pause