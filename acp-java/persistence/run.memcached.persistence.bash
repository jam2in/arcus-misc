#!/bin/bash

# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`
MEMC_DIR_NAME=arcus-memcached
MEMC_DIR=$DIR/../../../$MEMC_DIR_NAME
thread_count=6
mem_limit=2048
sleep_time=3
touch $MEMC_DIR_NAME.log

if [ -z "$1" ];
then
  port_num=11446
else
  port_num="$1"
fi

if [ -z "$2" ];
then
  conf_file=$MEMC_DIR/engines/default/default_engine.conf
else 
  conf_file="$DIR/$2"
fi

#conf_file path length must not be long.
USE_SYSLOG=0

if [ $USE_SYSLOG -eq 1 ];
then
  echo $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -e config_file=$conf_file -d -v -r -R5 -U 0 -D: -b 8192 -p $port_num -c 1000 -t $thread_count -m $mem_limit
  $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -X $MEMC_DIR/.libs/syslog_logger.so -X $MEMC_DIR/.libs/ascii_scrub.so -e config_file=$conf_file -d -v -r -R5 -U 0 -D: -b 8192 -p $port_num -c 1000 -t $thread_count -m $mem_limit
else
  echo $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -e config_file=$conf_file -d -v -r -R5 -U 0 -D: -b 8192 -p $port_num -c 1000 -t $thread_count -m $mem_limit
  $MEMC_DIR/memcached -E $MEMC_DIR/.libs/default_engine.so -e config_file=$conf_file -d -v -r -R5 -U 0 -D: -b 8192 -p $port_num -c 1000 -t $thread_count -m $mem_limit
fi
