@echo off
if not defined SERVER_PORT set SERVER_PORT=20330

call gradlew.bat bootJar -q
if errorlevel 1 exit /b 1

for %%f in (build\libs\*.jar) do set JAR_FILE=%%f

java -jar "%JAR_FILE%" --server.port=%SERVER_PORT%
