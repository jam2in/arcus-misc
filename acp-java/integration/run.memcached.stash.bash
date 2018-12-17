#!/bin/bash

if [ -z "$1" ];
then
  port_num=11211
else
  port_num=$1
fi

if [ -z "$2" ];
then
  repl_mode="sync"
else
  repl_mode=$2
fi

if [ -z "$3" ];
then
  zk_ip="127.0.0.1"
else
  zk_ip=$3
fi

# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`
MEMC_DIR_NAME=arcus-memcached
MEMC_DIR=$DIR/../../../$MEMC_DIR_NAME
thread_count=6
sleep_time=3

touch $MEMC_DIR_NAME.log

USE_SYSLOG=1

while :
do
  if [ $repl_mode == "sync" ];
  then
    $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -d -w -v -r -R5 -U 0 -D: -b 8192 -m2048 -p $port_num -c 1000 -t $thread_count -z $zk_ip:9181 -e "replication_config_file=integration/repl.sync.config;" -p $port_num -o 3 -g 100
    echo "$MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -d -w -v -r -R5 -U 0 -D: -b 8192 -m2048 -p $port_num -c 1000 -t $thread_count -z $zk_ip:9181 -e \"replication_config_file=integration/repl.sync.config;\" -p $port_num -o 3 -g 100"
  else # run as "async mode""
    $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -d -w -v -r -R5 -U 0 -D: -b 8192 -m2048 -p $port_num -c 1000 -t $thread_count -z $zk_ip:9181 -e "replication_config_file=integration/repl.async.config;" -p $port_num -o 3 -g 100
    echo "$MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -d -w -v -r -R5 -U 0 -D: -b 8192 -m2048 -p $port_num -c 1000 -t $thread_count -z $zk_ip:9181 -e \"replication_config_file=integration/repl.async.config;\" -p $port_num -o 3 -g 100"
  fi

  sleep $sleep_time
  is_run=`echo "stats replication" | nc 127.0.0.1 $port_num | grep "STAT mode" | wc -l`
  if [ "$is_run" == "1" ];
  then
    break
  fi
  echo ">>>>>> memcached.127.0.0.1:$port_num is not ready to run... Please wait a second..."
  #sleep 1
done
