echo Copying app components for distribution in distro directory...
mkdir distro
mkdir distro/client
mkdir distro/server
mkdir distro/androidclient

copy .\WordWatch\app\build\outputs\apk\debug\app-debug.apk .\distro\androidclient

copy json-simple-1.1.1.jar  .\distro\client
copy KeywordClient.jar  .\distro\client

copy KeywordServer\bin\KeywordServer.jar .\distro\server
copy json-simple-1.1.1.jar  .\distro\server
copy KeywordServer\start.sh .\distro\server
copy KeywordServer\start.cmd .\distro\server

echo Done.

