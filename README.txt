Welcome to the developers version of X-Trace 2.0. You most likely have checked this out of SVN since this is not the distribution package of X-Trace.

X-Trace is a framework for tracing the execution of distributed systems.  It
provides a logging-like API for the programmer, allowing the recording of
events during the execution.  The distinguishing feature of X-Trace is that it
records the causal relations among these events in a deterministic fashion,
across threads, software layers, different machines, and potentially different
network layers and administrative domains. X-Trace groups events in tasks, which
are sets of causally related events with a definite start. Events in a task for
a directed acyclic graph (DAG) which you can visualize and run analysis on.

X-Trace libraries have been implemented in a variety of languages. The primary X-Trace collection backend framework is implemented in java. See src/java/README.txt and src/java/INSTALL.txt for more information regarding the Java based X-Trace framework and instrumentation libraries.
