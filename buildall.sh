echo ...Building server...
cd KeywordServer
sh build.sh
cd ..
echo ...Building client...
cd KeywordClient
sh build.sh
cd ..
echo ...Server and client API build.
echo ...Updating Android client API jar file
mkdir WordWatch/libs
cp KeywordClient.jar WordWatch/libs
cp json-simple-1.1.1.jar WordWatch/libs
echo ...Now build the Android client app from within Android Studio.
