#!/bin/bash

cd `dirname $_`
cd ..
SCRIPT_NAME=`pwd`
BASE=`dirname $SCRIPT_NAME`

if [ $# != 0 ]; then
  if [ $1 == "-h" ]; then
    echo -e "Usage: backend.sh [X-Trace/data/dir]\nNOTE: default directory is ../data"
    exit
  else
    if [ -d $1 ]; then  
      DATA_DIR=$1
    else
      echo "WARNING: Invalid data directory provided, using default ($BASE/data) instead"
      DATA_DIR=$BASE/data
    fi
  fi
else
  echo "Using default data directory: $BASE/data"
  DATA_DIR=$BASE/data
fi

if [ ! -d $DATA_DIR ]; then
  mkdir -p $DATA_DIR
fi

CLASSPATH=$BASE/java/build/classes/:$BASE/lib
for i in `ls lib`; do
 CLASSPATH=./lib/$i:$CLASSPATH
done

java -Dlog4j.configuration=log4j-server.properties -Dxtrace.backend.webui.dir=$BASE/../webui -cp $CLASSPATH edu.berkeley.xtrace.server.XTraceServer $DATA_DIR
