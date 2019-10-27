
# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`
if test -d "$DIR/../../arcus-java-client" ; then
  JARFILE=$DIR/../../arcus-java-client/target/arcus-java-client-1.11.5.jar
else
  if test -d "$DIR/../../java-memcached-client" ; then
    JARFILE=$DIR/../../java-memcached-client/target/arcus-client-1.6.3.0.jar
  else
    echo "Cannot find arcus jar file."
    exit
  fi
fi

echo "Jar is at " $JARFILE

javac -Xlint:deprecation -classpath $JARFILE *.java
