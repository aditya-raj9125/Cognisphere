$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Clear-Host
Write-Host "Starting CogniSphere Backend..." -ForegroundColor Green
cd backend
mvn spring-boot:run -DskipTests -ntp
