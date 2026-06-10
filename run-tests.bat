@echo off
echo Running tests for Simple Calendar Application...
echo.

set MAVEN_CMD=.mvn\apache-maven-3.9.6\bin\mvn.cmd

call %MAVEN_CMD% test
pause