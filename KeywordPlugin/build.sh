mkdir bin
javac src\org\keyword\plugin\*.java  -d bin
cd bin
jar -cvfm KeywordPlugin.jar ..\src\META-INF\MANIFEST.MF org\keyword\plugin\*.class

