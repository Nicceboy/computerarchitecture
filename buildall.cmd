echo ...Building server...
cd .\KeywordServer
start /B /WAIT build.cmd
copy bin\KeywordServer.jar ..\build\ExampleServer
cd ..
echo ...Building KeywordPlugin API...
cd .\KeywordPlugin
start /B /WAIT build.cmd
copy bin\KeywordPlugin.jar ..\build\ExampleServer\lib\
cd ..
echo ...Building DirectoryWatcherPlugin...
cd .\DirectoryWatcherPlugin
start /B /WAIT build.cmd
copy bin\DirectoryWatcherPlugin.jar ..\build\ExampleServer\plugins\
cd ..
echo ...Building SamplePlugin...
cd .\SamplePlugin
start /B /WAIT build.cmd
copy bin\SamplePlugin.jar ..\build\ExampleServer\plugins\
cd ..
echo ...Building SampleAPI...
cd .\SampleAPI
start /B /WAIT build.cmd
copy bin\SampleAPI.jar ..\build\ExampleClient\lib\
cd ..
echo ...Building SampleClient...
cd .\SampleClient
start /B /WAIT build.cmd
copy bin\SampleClient.jar ..\build\ExampleClient\
cd ..
echo ...Copying libraries...
copy libs\json-simple-1.1.1.jar build\ExampleClient\lib\
copy libs\json-simple-1.1.1.jar build\ExampleServer\lib\
echo ...All files compiled and copied..
echo ...They are located in build folder. Server and client ready to be executed.




