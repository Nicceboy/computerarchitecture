mkdir bin
javac src\org\directorywatcher\plugin\*.java -classpath "..\libs\KeywordPlugin.jar;." -d bin
cd bin
jar -cvfm DirectoryWatcherPlugin.jar ..\src\META-INF\MANIFEST.MF org\directorywatcher\plugin\*.class  

