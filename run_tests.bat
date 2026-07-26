@echo off
setlocal
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PROJECT=C:\Users\rezau\eclipse-workspace\ny-coffee-pos
set MYSQL_JAR=%PROJECT%\lib\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0.jar

echo Compiling main sources...
if not exist "%PROJECT%\target\classes" mkdir "%PROJECT%\target\classes"
dir /s /b "%PROJECT%\src\com\*.java" > "%TEMP%\mainsrcs.txt"
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -nowarn -d "%PROJECT%\target\classes" -cp "%MYSQL_JAR%" @"%TEMP%\mainsrcs.txt"
if errorlevel 1 goto :error

echo Compiling test sources...
if not exist "%PROJECT%\target\test-classes" mkdir "%PROJECT%\target\test-classes"
dir /s /b "%PROJECT%\src\test\*.java" > "%TEMP%\testsrcs.txt"
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -nowarn -d "%PROJECT%\target\test-classes" -cp "%PROJECT%\target\classes;%PROJECT%\lib\junit5\*" @"%TEMP%\testsrcs.txt"
if errorlevel 1 goto :error

echo Running tests...
"%JAVA_HOME%\bin\java" -cp "%PROJECT%\target\classes;%PROJECT%\target\test-classes;%PROJECT%\lib\junit5\*" org.junit.platform.console.ConsoleLauncher --classpath "%PROJECT%\target\test-classes" --scan-classpath

goto :eof

:error
echo Compilation failed.
exit /b 1
