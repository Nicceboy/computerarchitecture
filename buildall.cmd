echo ...Building server...
cd .\KeywordServer
start /B /WAIT build.cmd
cd ..
echo ...Building client...
cd .\KeywordClient
start /B /WAIT build.cmd
cd ..
echo ...Server and client API build.
echo ...Updating Android client API jar file
mkdir WordWatch\libs
copy KeywordClient.jar WordWatch\libs
copy json-simple-1.1.1.jar WordWatch\libs
echo ...Now add the libs to WordWatch Android Project and 
echo ...build the Android client app from within Android Studio.

