@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

cls
echo Starting CogniSphere Backend...
cd backend
mvn spring-boot:run -DskipTests -ntp
