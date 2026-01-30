@echo off
setlocal
cd /D %~dp0
set CP=simple-table-server-alone-${version}-jar-with-dependencies.jar
java -jar %CP% %*
