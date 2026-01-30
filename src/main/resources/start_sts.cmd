setlocal
cd /D %~dp0
set CP=sts-alone-${version}-jar-with-dependencies.jar
java -jar %CP% %*
