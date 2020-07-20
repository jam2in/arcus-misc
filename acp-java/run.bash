# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`
if test -d "$DIR/../../arcus-java-client" ; then
  JAR_DIR=$DIR/../../arcus-java-client/target
  CP=$JAR_DIR/arcus-java-client-1.12.0.jar:$JAR_DIR/zookeeper-3.4.14.jar:$JAR_DIR/log4j-core-2.8.2.jar:$JAR_DIR/log4j-api-2.8.2.jar:$JAR_DIR/log4j-slf4j-impl-2.8.2.jar:$JAR_DIR/slf4j-api-1.7.24.jar
else
  if test -d "$DIR/../java-memcached-client" ; then
    JAR_DIR=$DIR/../java-memcached-client/target
    CP=$JAR_DIR/arcus-client-1.6.3.0.jar:$JAR_DIR/zookeeper-3.3.3-p1.jar:$JAR_DIR/log4j-1.2.16.jar
  else
    echo "Cannot find arcus jar directory."
    exit
  fi
fi

echo "Jar directory:" $JAR_DIR

java -Xmx2g -Xms2g "-Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.Log4JLogger" -classpath $CP:. acp $@
