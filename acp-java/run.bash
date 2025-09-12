#!/bin/bash

JAR_DIR="$HOME/arcus-java-client-version/develop/target"
CP="$JAR_DIR/*"
echo "Jar directory:" $JAR_DIR

java -Xmx2g -Xms2g "-Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.Log4JLogger" -classpath $CP:. acp $@
