mkdir bin
javac src\org\sample\plugin\*.java -classpath "..\libs\KeywordPlugin.jar;." -d bin
cd bin
jar -cvfm SamplePlugin.jar ..\src\META-INF\MANIFEST.MF org\sample\plugin\*.class
