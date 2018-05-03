mkdir bin
javac src/org/anttijuustila/keywordserver/*.java -classpath "../json-simple-1.1.1.jar:." -d bin
cd bin
jar cvf KeywordServer.jar org/anttijuustila/keywordserver/*.class

