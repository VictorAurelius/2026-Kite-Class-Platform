@REM Maven Wrapper for Windows
@REM Simplified version

@echo off
setlocal

set MAVEN_VERSION=3.9.6
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd

if not exist "%MAVEN_BIN%" (
    echo Downloading Maven %MAVEN_VERSION%...
    mkdir "%MAVEN_HOME%" 2>nul
    powershell -Command "& { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%TEMP%\maven.zip'; Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%TEMP%\maven-extract' -Force; Move-Item -Path '%TEMP%\maven-extract\apache-maven-%MAVEN_VERSION%\*' -Destination '%MAVEN_HOME%' -Force }"
)

"%MAVEN_BIN%" %* -f "%~dp0pom.xml"
