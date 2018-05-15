mkdir bin
javac src\org\sample\api\*.java -classpath "..\libs\json-simple-1.1.1.jar;" -d bin
cd bin
jar -cvfe SampleAPI.jar org.sample.SampleAPI.SampleAPI org\sample\api\*.class 
exit

