X-Trace Java Distribution 2.0

X-Trace is a framework for tracing the execution of distributed systems.  It
provides a logging-like API for the programmer, allowing the recording of
events during the execution.  The distinguishing feature of X-Trace is that it
records the causal relations among these events in a deterministic fashion,
across threads, software layers, different machines, and potentially different
network layers and administrative domains. X-Trace groups events in tasks, which are
sets of causally related events with a definite start. Events in a task for a 
directed acyclic graph (DAG) which you can visualize and run analysis on.

X-Trace has three basic parts:

  1. A library that you can use to add tracing to your own programs
        lib/xtrace-2.0.jar

  2. A daemon that runs on each machine where you have one or more programs
     being traced
        bin/proxy.sh

  3. A server program that receives reports from the local daemons and presents
     them on a web interface.
        bin/backend.sh

The library for adding tracing to your programs is documented in the javadoc files,
which can be obtained from the source (see INSTALL.txt) or at the X-Trace web site,
www.x-trace.net. The website also has a tutorial section on how to instrument your 
programs.

Each program instrumented with X-Trace will generate reports in one of three ways:
in UDP messages, in TCP messages, and in Thrift messages. (For details on thrift, see
xtrace.thrift and http://developers.facebook.com/thrift/). The default is UDP messages
to port 7831. The proxy listens for these and forwards them to the backend. You can
in fact change the configuration and even bypass the proxy, but in general it won't be
a good idea.

The backend allows you to get the reports for specific tasks in raw format, as well as
visualize them in a graph. We are working on a set of tools to make analyzing X-Trace
task graphs easier.

There is also a similar library for C++ programs, that can be downloaded in beta
format from www.x-trace.net, and will be included in the next version of this 
distribution.

For more details, including a tutorial, a copy of the javadoc, and a scientific paper that
describes X-Trace, please visit www.x-trace.net.

--

