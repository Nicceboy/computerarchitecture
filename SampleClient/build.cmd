mkdir bin
javac src\org\sample\client\*.java -classpath "..\libs\json-simple-1.1.1.jar;..\SampleAPI\bin\SampleAPI.jar" -d bin
cd bin
jar -cvfm SampleClient.jar ..\src\META-INF\MANIFEST.MF org\sample\client\*.class
exit

