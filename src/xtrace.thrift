#!/usr/local/bin/thrift -cpp -java

cpp_namespace xtrace
java_package edu.berkeley.xtrace.reporting

service XTraceReporter {
  
  /**
   * A method definition looks like C code. It has a return type, arguments,
   * and optionally a list of exceptions that it may throw. Note that argument
   * lists and exception lists are specified using the exact same syntax as
   * field lists in struct or exception definitions.
   */

   void ping(),

   void sendReport(1:string report)
}
