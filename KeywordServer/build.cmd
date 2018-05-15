mkdir bin
javac src\org\anttijuustila\keywordserver\*.java -classpath "..\libs\json-simple-1.1.1.jar;..\libs\KeywordPlugin.jar;" -d bin
cd bin
jar -cvfm KeywordServer.jar ..\src\META-INF\MANIFEST.MF org\anttijuustila\keywordserver\*.class  
exit

