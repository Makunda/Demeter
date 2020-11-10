@echo off
set FILENAME=%1
set SERVICE_NAME = "imaging-neo4j"
set FILEPATH="%2\%1"
set DEST_FOLDER="C:/Program Files/CAST/ImagingSystem/neo4j/plugins/%FILENAME%"
echo Copying %FILEPATH% to %DEST_FOLDER%
copy %FILEPATH% %DEST_FOLDER% /Y
echo "Done !"
echo "Service restarting..."


sc query imaging-neo4j | find /I "STATE" | find "STOPPED"
if errorlevel 1 goto :stop
goto :start

:stop
net stop imaging-neo4j

:start
net start imaging-neo4j
