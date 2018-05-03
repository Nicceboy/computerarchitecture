mkdir bin
javac -classpath "../json-simple-1.1.1.jar:." -d bin src/org/anttijuustila/keywordclient/*.java
cd bin
jar cvf ../../KeywordClient.jar org/anttijuustila/keywordclient/*.class
