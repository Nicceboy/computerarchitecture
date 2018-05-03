echo Copying app components for distribution into distro directory...
mkdir distro
mkdir distro/client
mkdir distro/server
mkdir distro/androidclient

cp ./WordWatch/app/build/outputs/apk/debug/app-debug.apk ./distro/androidclient

cp json-simple-1.1.1.jar  ./distro/client
cp KeywordClient.jar  ./distro/client

cp KeywordServer/bin/KeywordServer.jar ./distro/server
cp json-simple-1.1.1.jar  ./distro/server
cp KeywordServer/start.sh ./distro/server

echo Done.

