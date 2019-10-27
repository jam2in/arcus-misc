# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`
 if test -d "$DIR/../../arcus-java-client" ; then
  JAR_DIR=$DIR/../../arcus-java-client/target
  CP=$JAR_DIR/arcus-java-client-1.11.5.jar:$JAR_DIR/zookeeper-3.4.5.jar:$JAR_DIR/log4j-1.2.16.jar:$JAR_DIR/slf4j-api-1.6.1.jar:$JAR_DIR/slf4j-log4j12-1.6.1.jar
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

java -ea -Xmx3g -Xms3g "-Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.Log4JLogger" -classpath $CP:. acp $@
