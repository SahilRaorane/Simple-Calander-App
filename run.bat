@echo off
@REM Set JAVA_HOME to JDK if not already pointing to one with javac
if not exist "%JAVA_HOME%\bin\javac.exe" (
    set JAVA_HOME=C:\Program Files\Java\jdk-1.8
)
echo Building Simple Calendar Application...
call .mvn\apache-maven-3.9.6\bin\mvn.cmd clean package -q
if %ERRORLEVEL% NEQ 0 (
    echo Build failed. Please check the output above.
    pause
    exit /b 1
)
echo Build successful! Starting application...
echo.
java -jar target\calendar-app.jar
