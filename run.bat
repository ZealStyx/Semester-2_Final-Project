@echo off
setlocal enabledelayedexpansion
title Resonance — Dev Launcher

:MAIN_MENU
cls
echo.
echo  ==========================================
echo   RESONANCE  ^|  Dev Launcher
echo  ==========================================
echo.
echo   [1]  Run Game (lwjgl3)
echo   [2]  Run Server
echo   [3]  Build
echo   [4]  Clean
echo   [5]  Tests
echo   [6]  JARs ^& Packaging
echo   [7]  Gradle Info
echo   [8]  Dependency Management
echo   [0]  Exit
echo.
echo  ==========================================
set /p MAIN="  Choose: "

if "%MAIN%"=="1" goto MENU_RUN
if "%MAIN%"=="2" goto MENU_SERVER
if "%MAIN%"=="3" goto MENU_BUILD
if "%MAIN%"=="4" goto MENU_CLEAN
if "%MAIN%"=="5" goto MENU_TESTS
if "%MAIN%"=="6" goto MENU_JAR
if "%MAIN%"=="7" goto MENU_INFO
if "%MAIN%"=="8" goto MENU_DEPS
if "%MAIN%"=="0" goto EXIT
goto MAIN_MENU

:: ==========================================
:MENU_RUN
cls
echo.
echo  ==========================================
echo   Run Game
echo  ==========================================
echo.
echo   [1]  Run (normal)
echo   [2]  Run with debug logging
echo   [3]  Run with extra JVM memory (2GB)
echo   [4]  Run with profiler port open (5005)
echo   [5]  Run — skip asset regeneration
echo   [0]  Back
echo.
echo  ==========================================
set /p RUN="  Choose: "

if "%RUN%"=="1" (
    echo.
    echo  Starting Resonance...
    echo.
    call gradlew.bat :lwjgl3:run
    goto DONE
)
if "%RUN%"=="2" (
    echo.
    echo  Starting Resonance with debug logging...
    echo.
    call gradlew.bat :lwjgl3:run --info
    goto DONE
)
if "%RUN%"=="3" (
    echo.
    echo  Starting Resonance with 2GB heap...
    echo.
    call gradlew.bat :lwjgl3:run -Dorg.gradle.jvmargs="-Xms512M -Xmx2G"
    goto DONE
)
if "%RUN%"=="4" (
    echo.
    echo  Starting Resonance in debug mode (suspend=n, port 5005)...
    echo  Attach your debugger to localhost:5005
    echo.
    call gradlew.bat :lwjgl3:run --debug-jvm
    goto DONE
)
if "%RUN%"=="5" (
    echo.
    echo  Starting Resonance (skipping asset list generation)...
    echo.
    call gradlew.bat :lwjgl3:run -x generateAssetList
    goto DONE
)
if "%RUN%"=="0" goto MAIN_MENU
goto MENU_RUN

:: ==========================================
:MENU_SERVER
cls
echo.
echo  ==========================================
echo   Server
echo  ==========================================
echo.
echo   [1]  Run server
echo   [2]  Run server with debug logging
echo   [3]  Build server only
echo   [4]  Build server JAR
echo   [0]  Back
echo.
echo  ==========================================
set /p SRV="  Choose: "

if "%SRV%"=="1" (
    echo.
    echo  Starting server...
    echo.
    call gradlew.bat :server:run
    goto DONE
)
if "%SRV%"=="2" (
    echo.
    echo  Starting server with debug logging...
    echo.
    call gradlew.bat :server:run --info
    goto DONE
)
if "%SRV%"=="3" (
    echo.
    echo  Building server module...
    echo.
    call gradlew.bat :server:build
    goto DONE
)
if "%SRV%"=="4" (
    echo.
    echo  Building server JAR...
    echo.
    call gradlew.bat :server:jar
    goto DONE
)
if "%SRV%"=="0" goto MAIN_MENU
goto MENU_SERVER

:: ==========================================
:MENU_BUILD
cls
echo.
echo  ==========================================
echo   Build
echo  ==========================================
echo.
echo   [1]  Build all modules
echo   [2]  Build core only
echo   [3]  Build lwjgl3 only
echo   [4]  Build server only
echo   [5]  Build shared only
echo   [6]  Compile only (no tests)
echo   [7]  Build — skip tests
echo   [8]  Build with stacktrace on failure
echo   [0]  Back
echo.
echo  ==========================================
set /p BLD="  Choose: "

if "%BLD%"=="1" (
    echo.
    echo  Building all modules...
    echo.
    call gradlew.bat build
    goto DONE
)
if "%BLD%"=="2" (
    echo.
    echo  Building core...
    echo.
    call gradlew.bat :core:build
    goto DONE
)
if "%BLD%"=="3" (
    echo.
    echo  Building lwjgl3...
    echo.
    call gradlew.bat :lwjgl3:build
    goto DONE
)
if "%BLD%"=="4" (
    echo.
    echo  Building server...
    echo.
    call gradlew.bat :server:build
    goto DONE
)
if "%BLD%"=="5" (
    echo.
    echo  Building shared...
    echo.
    call gradlew.bat :shared:build
    goto DONE
)
if "%BLD%"=="6" (
    echo.
    echo  Compiling all (no test compilation)...
    echo.
    call gradlew.bat compileJava
    goto DONE
)
if "%BLD%"=="7" (
    echo.
    echo  Building all (skipping tests)...
    echo.
    call gradlew.bat build -x test
    goto DONE
)
if "%BLD%"=="8" (
    echo.
    echo  Building all with full stacktrace...
    echo.
    call gradlew.bat build --stacktrace
    goto DONE
)
if "%BLD%"=="0" goto MAIN_MENU
goto MENU_BUILD

:: ==========================================
:MENU_CLEAN
cls
echo.
echo  ==========================================
echo   Clean
echo  ==========================================
echo.
echo   [1]  Clean all modules
echo   [2]  Clean core only
echo   [3]  Clean lwjgl3 only
echo   [4]  Clean server only
echo   [5]  Clean shared only
echo   [6]  Clean then build all
echo   [7]  Clean then run
echo   [8]  Nuke Gradle cache (use when deps are broken)
echo   [0]  Back
echo.
echo  ==========================================
set /p CLN="  Choose: "

if "%CLN%"=="1" (
    echo.
    echo  Cleaning all modules...
    echo.
    call gradlew.bat clean
    goto DONE
)
if "%CLN%"=="2" (
    echo.
    echo  Cleaning core...
    echo.
    call gradlew.bat :core:clean
    goto DONE
)
if "%CLN%"=="3" (
    echo.
    echo  Cleaning lwjgl3...
    echo.
    call gradlew.bat :lwjgl3:clean
    goto DONE
)
if "%CLN%"=="4" (
    echo.
    echo  Cleaning server...
    echo.
    call gradlew.bat :server:clean
    goto DONE
)
if "%CLN%"=="5" (
    echo.
    echo  Cleaning shared...
    echo.
    call gradlew.bat :shared:clean
    goto DONE
)
if "%CLN%"=="6" (
    echo.
    echo  Cleaning then building all modules...
    echo.
    call gradlew.bat clean build
    goto DONE
)
if "%CLN%"=="7" (
    echo.
    echo  Cleaning then running game...
    echo.
    call gradlew.bat clean :lwjgl3:run
    goto DONE
)
if "%CLN%"=="8" (
    echo.
    echo  WARNING: This will delete your local Gradle cache.
    set /p CONFIRM="  Are you sure? (y/n): "
    if /i "!CONFIRM!"=="y" (
        echo  Deleting .gradle cache...
        if exist ".gradle" rmdir /s /q ".gradle"
        echo  Stopping Gradle daemon...
        call gradlew.bat --stop
        echo  Done. Run a build to re-download dependencies.
    ) else (
        echo  Cancelled.
    )
    goto DONE
)
if "%CLN%"=="0" goto MAIN_MENU
goto MENU_CLEAN

:: ==========================================
:MENU_TESTS
cls
echo.
echo  ==========================================
echo   Tests
echo  ==========================================
echo.
echo   [1]  Run all tests
echo   [2]  Run core tests only
echo   [3]  Run tests with verbose output
echo   [4]  Run a specific test class
echo   [5]  Run tests and open HTML report
echo   [0]  Back
echo.
echo  ==========================================
set /p TST="  Choose: "

if "%TST%"=="1" (
    echo.
    echo  Running all tests...
    echo.
    call gradlew.bat test
    goto DONE
)
if "%TST%"=="2" (
    echo.
    echo  Running core tests...
    echo.
    call gradlew.bat :core:test
    goto DONE
)
if "%TST%"=="3" (
    echo.
    echo  Running all tests (verbose)...
    echo.
    call gradlew.bat test --info
    goto DONE
)
if "%TST%"=="4" (
    echo.
    set /p TESTCLASS="  Enter fully-qualified class name (e.g. io.github.superteam.resonance.sound.MicInputListenerTest): "
    echo.
    echo  Running !TESTCLASS!...
    echo.
    call gradlew.bat :core:test --tests "!TESTCLASS!"
    goto DONE
)
if "%TST%"=="5" (
    echo.
    echo  Running all tests...
    echo.
    call gradlew.bat test
    echo.
    echo  Opening test report...
    if exist "core\build\reports\tests\test\index.html" (
        start "" "core\build\reports\tests\test\index.html"
    ) else (
        echo  Report not found at core\build\reports\tests\test\index.html
    )
    goto DONE
)
if "%TST%"=="0" goto MAIN_MENU
goto MENU_TESTS

:: ==========================================
:MENU_JAR
cls
echo.
echo  ==========================================
echo   JARs ^& Packaging
echo  ==========================================
echo.
echo   [1]  Build distributable JAR (all platforms)
echo   [2]  Build JAR — Windows only (smaller)
echo   [3]  Build JAR — macOS M1 only
echo   [4]  Build JAR — macOS Intel only
echo   [5]  Build JAR — Linux only
echo   [6]  Build server JAR
echo   [7]  Build installer (.exe / .msi)
echo   [8]  Show output JAR location
echo   [0]  Back
echo.
echo  ==========================================
set /p JAR="  Choose: "

if "%JAR%"=="1" (
    echo.
    echo  Building cross-platform distributable JAR...
    echo.
    call gradlew.bat :lwjgl3:dist
    echo.
    echo  Output: lwjgl3\build\libs\
    goto DONE
)
if "%JAR%"=="2" (
    echo.
    echo  Building Windows-only JAR...
    echo.
    call gradlew.bat :lwjgl3:jarWin
    echo.
    echo  Output: lwjgl3\build\libs\
    goto DONE
)
if "%JAR%"=="3" (
    echo.
    echo  Building macOS M1 JAR...
    echo.
    call gradlew.bat :lwjgl3:jarMac
    echo.
    echo  Output: lwjgl3\build\libs\
    goto DONE
)
if "%JAR%"=="4" (
    echo.
    echo  Building macOS Intel JAR...
    echo.
    call gradlew.bat :lwjgl3:jarMac
    echo.
    echo  Output: lwjgl3\build\libs\
    goto DONE
)
if "%JAR%"=="5" (
    echo.
    echo  Building Linux JAR...
    echo.
    call gradlew.bat :lwjgl3:jarLinux
    echo.
    echo  Output: lwjgl3\build\libs\
    goto DONE
)
if "%JAR%"=="6" (
    echo.
    echo  Building server JAR...
    echo.
    call gradlew.bat :server:jar
    echo.
    echo  Output: server\build\libs\
    goto DONE
)
if "%JAR%"=="7" (
    echo.
    echo  Launching installer_maker.bat...
    echo.
    call installer_maker.bat
    goto MAIN_MENU
)
if "%JAR%"=="8" (
    echo.
    echo  JAR output locations:
    echo.
    if exist "lwjgl3\build\libs" (
        dir /b "lwjgl3\build\libs\*.jar" 2>nul && echo  (in lwjgl3\build\libs\) || echo  No JARs found in lwjgl3\build\libs\
    ) else (
        echo  lwjgl3\build\libs\ does not exist yet — run a build first.
    )
    if exist "server\build\libs" (
        dir /b "server\build\libs\*.jar" 2>nul && echo  (in server\build\libs\) || echo  No JARs found in server\build\libs\
    )
    goto DONE
)
if "%JAR%"=="0" goto MAIN_MENU
goto MENU_JAR

:: ==========================================
:MENU_INFO
cls
echo.
echo  ==========================================
echo   Gradle Info
echo  ==========================================
echo.
echo   [1]  List all available tasks
echo   [2]  List tasks for a specific module
echo   [3]  Show project dependencies (all)
echo   [4]  Show core dependencies
echo   [5]  Show lwjgl3 dependencies
echo   [6]  Show Gradle version
echo   [7]  Show Java version
echo   [8]  Show project properties
echo   [9]  Show build environment
echo   [0]  Back
echo.
echo  ==========================================
set /p INF="  Choose: "

if "%INF%"=="1" (
    echo.
    call gradlew.bat tasks --all
    goto DONE
)
if "%INF%"=="2" (
    echo.
    echo  Available modules: core, lwjgl3, server, shared
    set /p MOD="  Module name: "
    echo.
    call gradlew.bat :!MOD!:tasks
    goto DONE
)
if "%INF%"=="3" (
    echo.
    call gradlew.bat dependencies
    goto DONE
)
if "%INF%"=="4" (
    echo.
    call gradlew.bat :core:dependencies
    goto DONE
)
if "%INF%"=="5" (
    echo.
    call gradlew.bat :lwjgl3:dependencies
    goto DONE
)
if "%INF%"=="6" (
    echo.
    call gradlew.bat --version
    goto DONE
)
if "%INF%"=="7" (
    echo.
    java -version
    goto DONE
)
if "%INF%"=="8" (
    echo.
    call gradlew.bat properties
    goto DONE
)
if "%INF%"=="9" (
    echo.
    call gradlew.bat buildEnvironment
    goto DONE
)
if "%INF%"=="0" goto MAIN_MENU
goto MENU_INFO

:: ==========================================
:MENU_DEPS
cls
echo.
echo  ==========================================
echo   Dependency Management
echo  ==========================================
echo.
echo   [1]  Check for dependency updates
echo   [2]  Refresh dependencies (re-download)
echo   [3]  Show dependency insight for a lib
echo   [4]  Export dependency tree to file
echo   [0]  Back
echo.
echo  ==========================================
set /p DEP="  Choose: "

if "%DEP%"=="1" (
    echo.
    echo  Checking for outdated dependencies...
    echo  (Uses Gradle --refresh-dependencies flag)
    echo.
    call gradlew.bat dependencyUpdates --refresh-dependencies
    goto DONE
)
if "%DEP%"=="2" (
    echo.
    echo  Re-downloading all dependencies...
    echo.
    call gradlew.bat build --refresh-dependencies
    goto DONE
)
if "%DEP%"=="3" (
    echo.
    set /p DEPNAME="  Enter dependency (e.g. com.badlogicgames.gdx:gdx): "
    echo.
    call gradlew.bat :core:dependencyInsight --dependency !DEPNAME!
    goto DONE
)
if "%DEP%"=="4" (
    echo.
    echo  Writing dependency tree to dep_tree.txt...
    echo.
    call gradlew.bat dependencies > dep_tree.txt
    echo  Saved to dep_tree.txt
    goto DONE
)
if "%DEP%"=="0" goto MAIN_MENU
goto MENU_DEPS

:: ==========================================
:DONE
echo.
echo  ==========================================
echo.
set /p AGAIN="  Back to menu? (y/n): "
if /i "%AGAIN%"=="y" goto MAIN_MENU
if /i "%AGAIN%"=="n" goto EXIT
goto DONE

:EXIT
echo.
echo  Bye!
echo.
endlocal
exit /b 0