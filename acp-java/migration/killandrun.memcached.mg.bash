#!/bin/bash

if [ -z "$1" ];
then
  pidfile="master"
else
  pidfile="$1"
fi

if [ -z "$2" ];
then
  port_num=11211
else
  port_num=$2
fi

if [ -z "$3" ];
then
  kill_type="INT"
else
  kill_type="$3"
fi

# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`
MEMC_DIR_NAME=arcus-memcached-ee
#MEMC_DIR=$DIR/../../../$MEMC_DIR_NAME
#MEMC_DIR=$DIR/../../../../migration-ref/$MEMC_DIR_NAME
MEMC_DIR=$DIR/../../../../migration-test/$MEMC_DIR_NAME
thread_count=6
sleep_time=3
do_not_kill_async_master=1

touch $MEMC_DIR_NAME.log

mkdir -p pidfiles

if [ "$kill_type" != "NONE" ];
then
  if [ -f "pidfiles/memcached.127.0.0.1:$port_num" ];
  then
    while [ $do_not_kill_async_master -eq 1 ];
    do
      is_master=`echo "stats replication" | nc 127.0.0.1 $port_num | grep "mode master" | wc -l`
      if [ "$is_master" == "0" ];
      then
        break
      fi
      is_async=`echo "stats replication" | nc 127.0.0.1 $port_num | grep "state ASYNC_RUN" | wc -l`
      if [ "$is_async" == "0" ];
      then
        break
      fi
      echo ">>>>>> memcached.127.0.0.1:$port_num is master but in async... Please wait a second.."
      sleep 1
    done
    echo ">>>>>> kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$port_num`"
    kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$port_num`
  fi
fi

echo ">>>>>> start memcached as $pidfile... : `date`"
sleep 1

USE_SYSLOG=1

while :
do
  if [ $USE_SYSLOG -eq 1 ];
  then
    $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -d -v -r -R5 -U 0 -D: -b 8192 -m400 -p $port_num -c 1000 -t $thread_count -z 127.0.0.1:2181 -e "replication_config_file=replication.config;migration_config_file=migration.config" -P pidfiles/memcached.127.0.0.1:$port_num -o 3 -g 100
  else
    $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/ascii_scrub.so -d -v -r -R5 -U 0 -D: -b 8192 -m1000 -p $port_num -c 1000 -t $thread_count -z 127.0.0.1:2181 -e "replication_config_file=replication.config;" -P pidfiles/memcached.127.0.0.1:$port_num -o 3 -g 100 >> $MEMC_DIR_NAME.log 2>&1
  fi

  #sleep $sleep_time
  #is_run=`echo "stats replication" | nc 127.0.0.1 $port_num | grep "STAT mode" | wc -l`
  #if [ "$is_run" == "1" ];
  #then
    break
  #fi
  #echo ">>>>>> memcached.127.0.0.1:$port_num is not ready to run... Please wait a second..."
  #sleep 1
done
