#!/bin/sh
cd `dirname $0`
CP=simple-table-server-alone-${version}-jar-with-dependencies.jar
java -cp $CP $*