#!/bin/bash

BASE=`dirname ${0}`/..

CLASSPATH=$BASE/lib/xtrace-2.0.jar:$BASE/lib/

java -Dlog4j.configuration=log4j-server.properties -Dxtrace.backend.webui.dir=$BASE/webui -cp $CLASSPATH edu.berkeley.xtrace.server.XTraceServer $BASE/data
