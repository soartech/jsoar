(1) commons-logging-1.1.jar is required by flexdock, but is used generally by
jsoar. Commons logging defaults to java.util.Logger, but, if log4j is on the
classpath, it will use that instead.
(2) jsoar uses Google Collections API specifically for its support of
maps with weak keys/values. See SymbolTableImpl.