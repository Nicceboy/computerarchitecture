echo ...Building server...
cd KeywordServer
sh build.sh
cp bin/KeywordServer.jar ../build/ExampleServer
cd ..
echo ...Building KeywordPlugin API...
cd KeywordPlugin
sh build.sh
cp bin/KeywordPlugin.jar ../build/ExampleServer/lib/
cd ..
echo ...Building DirectoryWatcherPlugin...
cd DirectoryWatcherPlugin
sh build.sh
cp bin/DirectoryWatcherPlugin.jar ../build/ExampleServer/plugins/
cd ..
echo ...Building SamplePlugin...
cd SamplePlugin
sh build.sh
cp bin/SamplePlugin.jar ../build/ExampleServer/plugins/
cd ..
echo ...Building SampleAPI...
cd SampleAPI
sh build.sh
cp bin/SampleAPI.jar ../build/ExampleClient/lib/
cd ..
echo ...Building SampleClient...
cd SampleClient
sh build.sh
cp bin/SampleClient.jar ../build/ExampleClient/
cd ..
echo ...Copying libraries...
cp libs/json-simple-1.1.1.jar build/ExampleClient/lib/
cp libs/json-simple-1.1.1.jar build/ExampleServer/lib/
echo ...All files compiled and copied..
echo ...They are located in build folder. Server and client ready to be executed.
