@echo off
setlocal

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%tools\jdk-21.0.12+8"
set "MAVEN_HOME=%ROOT%tools\apache-maven-3.9.9"
set "MAVEN_REPO=%ROOT%tools\m2-repo"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Portable Java was not found at:
    echo %JAVA_HOME%
    echo Copy the tools directory into this project directory first.
    exit /b 1
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Portable Maven was not found at:
    echo %MAVEN_HOME%
    echo Copy the tools directory into this project directory first.
    exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" -f "%ROOT%game421\pom.xml" -Dmaven.repo.local="%MAVEN_REPO%" clean package
if errorlevel 1 exit /b %errorlevel%

"%JAVA_HOME%\bin\java.exe" -jar "%ROOT%game421\target\game421-1.0.0.jar"
