@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script (Windows)
@REM Auto-downloads Apache Maven 3.9.6 on first run.
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper
set WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

@REM -- Read distributionUrl from properties file
for /f "tokens=2 delims==" %%a in ('findstr /i "distributionUrl" "%WRAPPER_PROPERTIES%"') do (
    set DISTRIBUTION_URL=%%a
)

@REM -- Determine Maven home inside project folder
set MAVEN_HOME_LOCAL=%MAVEN_PROJECTBASEDIR%.mvn\maven

@REM -- Download and extract Maven if not yet present
if not exist "%MAVEN_HOME_LOCAL%\bin\mvn.cmd" (
    echo [mvnw] Maven not found locally. Downloading from Apache...
    set MAVEN_ZIP=%MAVEN_HOME_LOCAL%\..\maven-dist.zip
    powershell -Command "New-Item -ItemType Directory -Force '%MAVEN_HOME_LOCAL%' | Out-Null; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_HOME_LOCAL%\..\maven-dist.zip' -UseBasicParsing"
    powershell -Command "Expand-Archive -Path '%MAVEN_HOME_LOCAL%\..\maven-dist.zip' -DestinationPath '%MAVEN_HOME_LOCAL%\..' -Force"
    for /d %%d in ("%MAVEN_HOME_LOCAL%\..\apache-maven-*") do (
        if not "%%d"=="%MAVEN_HOME_LOCAL%" (
            ren "%%d" "maven"
        )
    )
    del "%MAVEN_HOME_LOCAL%\..\maven-dist.zip" 2>nul
    echo [mvnw] Maven downloaded successfully.
)

set PATH=%MAVEN_HOME_LOCAL%\bin;%PATH%
call "%MAVEN_HOME_LOCAL%\bin\mvn.cmd" %*
