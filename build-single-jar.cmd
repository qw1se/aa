@echo off
cd /d "%~dp0frontend"
call npm install
if errorlevel 1 goto fail
call npm run build
if errorlevel 1 goto fail
cd /d "%~dp0backend"
if exist src\main\resources\static rmdir /s /q src\main\resources\static
mkdir src\main\resources\static
xcopy /e /i /y ..\frontend\dist src\main\resources\static >nul
if errorlevel 1 goto fail
call mvn clean package
if errorlevel 1 goto fail
echo Built: backend\target\alchemy-web-1.0.0.jar
pause
exit /b 0
:fail
echo Build failed. See errors above.
pause
exit /b 1
