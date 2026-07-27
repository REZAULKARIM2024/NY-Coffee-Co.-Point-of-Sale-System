@echo off
setlocal
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PROJECT=C:\Users\rezau\eclipse-workspace\ny-coffee-pos
set MYSQL_JAR=%PROJECT%\lib\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0.jar
set PORT=8081

echo Compiling...
if not exist "%PROJECT%\target\classes" mkdir "%PROJECT%\target\classes"
dir /s /b "%PROJECT%\src\main\java\com\*.java" > "%TEMP%\mainsrcs.txt"
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -nowarn -d "%PROJECT%\target\classes" -cp "%MYSQL_JAR%" @"%TEMP%\mainsrcs.txt"
if errorlevel 1 goto :error

echo Starting API server on http://localhost:%PORT%/api/health ...
"%JAVA_HOME%\bin\java" -cp "%PROJECT%\target\classes;%MYSQL_JAR%" com.possystem.api.ApiServer %PORT%
goto :eof

:error
echo Compilation failed.
exit /b 1
