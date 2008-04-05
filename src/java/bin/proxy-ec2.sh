#!/bin/bash

set -u

if [ $# -ne 1 ]; then
        echo 1>&2 Usage: $0 xtraceServerHostname
        exit 127
fi

BASE=`dirname ${0}`/..

CLASSPATH=$BASE/lib/derby.jar:$BASE/lib/junit-4.4.jar:$BASE/lib/velocity-dep-1.5.jar:$BASE/lib/je-3.2.44.jar:$BASE/lib/log4j-1.2.15.jar:$BASE/lib/xtrace-2.0.jar:$BASE/lib/jetty-6.1.6.jar:$BASE/lib/servlet-api-2.5-6.1.6.jar:$BASE/lib/jetty-util-6.1.6.jar:$BASE/lib/velocity-1.5.jar:$BASE/lib/libthrift.jar:$BASE

JAVA_HOME=/usr/lib/jvm/java-1.5.0-sun /usr/lib/jvm/java-1.5.0-sun/bin/java -Dlog4j.configuration=lib/log4j-proxy.properties -Dxtrace.reporter="edu.berkeley.xtrace.reporting.ThriftReporter" -Dxtrace.thriftdest="${1}:7831" -cp $CLASSPATH edu.berkeley.xtrace.server.XTraceProxy
