#!/bin/bash

BASE=`dirname ${0}`/..

if [ $# != 0 ]; then
  if [ $1 == "-h" ]; then
    echo -e "Usage: backend.sh [X-Trace/data/dir]\nNOTE: default directory is /mnt/traces/data"
    exit
  else
    if [ -d $1 ]; then  
      DATA_DIR=$1
    else
      echo "WARNING: Invalid data directory provided, using default (/mnt/traces/data) instead"
      DATA_DIR=/mnt/traces/data
    fi
  fi
else
  echo "Using default data directory: /mnt/traces/data"
  DATA_DIR=/mnt/traces/data
fi

if [ ! -d $DATA_DIR ]; then
mkdir -p $DATA_DIR
fi

CLASSPATH=$BASE/lib/xtrace-2.0.jar:$BASE/lib/

JAVA_HOME=/usr/lib/jvm/java-1.5.0-sun /usr/lib/jvm/java-1.5.0-sun/bin/java -Dlog4j.configuration=log4j-server.properties -Dxtrace.backend.webui.dir=$BASE/webui -cp $CLASSPATH edu.berkeley.xtrace.server.XTraceServer $DATA_DIR
