#!/bin/bash

echo ">>>>>> $0 kill_type start_time run_interval run_count"

if [ -z "$1" ];
then
  kill_type="INT"
else
  kill_type=$1
fi

if [ -z "$2" ];
then
  start_time=5
else
  start_time=$2
fi

if [ -z "$3" ];
then
  run_interval=30
else
  run_interval=$3
fi

if [ -z "$4" ];
then
  run_count=1000000
else
  run_count=$4
fi

g0_m_port=11213
g0_s_port=11214
g1_m_port=11215
g1_s_port=11216
g2_m_port=11217
g2_s_port=11218
g3_m_port=11219
g3_s_port=11220
g4_m_port=11221
g4_s_port=11222

COUNTER=1

#kill exist node
function kill_exist() {
  echo ">>>>>> $0 running ($COUNTER/$run_count)"
  if [ -f "pidfiles/memcached.127.0.0.1:$g2_m_port" ];
  then
    echo ">>>>>> kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g2_m_port`"
    kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g2_m_port`
  fi

  if [ -f "pidfiles/memcached.127.0.0.1:$g2_s_port" ];
  then
    echo ">>>>>> kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g2_s_port`"
    kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g2_s_port`
  fi
}

function kill_exist_2() {
  echo ">>>>>> $0 running ($COUNTER/$run_count)"
  if [ -f "pidfiles/memcached.127.0.0.1:$g3_m_port" ];
  then
    echo ">>>>>> kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g3_m_port`"
    kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g3_m_port`
  fi

  if [ -f "pidfiles/memcached.127.0.0.1:$g3_s_port" ];
  then
    echo ">>>>>> kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g3_s_port`"
    kill -$kill_type `cat pidfiles/memcached.127.0.0.1:$g3_s_port`
  fi
}

function g0_stats() {
   echo stats | nc localhost $g0_m_port | grep curr_items
}

function g1_stats() {
   echo stats | nc localhost $g1_m_port | grep curr_items
}

function g2_stats() {
   echo stats | nc localhost $g2_m_port | grep curr_items
}

function g3_stats() {
   echo stats | nc localhost $g3_m_port | grep curr_items
}

function g4_stats() {
    echo stats | nc localhost $g4_m_port | grep curr_items
}

function print_item_count_some() {
  echo ">>> $g0_m_port items"
  g0_stats
  echo ">>> $g1_m_port items"
  g1_stats
  echo ">>> $g2_m_port items"
  g2_stats
  echo ">>> $g3_m_port items"
  g3_stats
}

g0_count=0
g1_count=0
g2_count=0
g3_count=0
g4_count=0

function print_item_count() {
  echo ">>> $g0_m_port items"
  g0_str=$(g0_stats)
  g0_sub=${g0_str:16}
  g0_count=`echo $g0_sub | sed 's/[^0-9]//g'`
  echo $g0_str

  echo ">>> $g1_m_port items"
  g1_str=$(g1_stats)
  g1_sub=${g1_str:16}
  g1_count=`echo $g1_sub | sed 's/[^0-9]//g'`
  echo $g1_str

  echo ">>> $g2_m_port items"
  g2_str=$(g2_stats)
  g2_sub=${g2_str:16}
  g2_count=`echo $g2_sub | sed 's/[^0-9]//g'`
  echo $g2_str

  echo ">>> $g3_m_port items"
  g3_str=$(g3_stats)
  g3_sub=${g3_str:16}
  g3_count=`echo $g3_sub | sed 's/[^0-9]//g'`
  echo $g3_str

  echo ">>> $g4_m_port items"
  g4_str=$(g4_stats)
  g4_sub=${g4_str:16}
  g4_count=`echo $g4_sub | sed 's/[^0-9]//g'`
  echo $g4_str
}

echo ">>>>>> $0 $master_port $slave_port $kill_type $start_time $run_interval $run_count"

#prepare migration node and item
echo "all migration node run"
./start_memcached_migration.bash

echo "g0 M-$g0_m_port, S-$g0_s_port migration join"
echo cluster join alone | nc localhost $g0_m_port

sleep 5

item_count=5000;
num=0;
echo prepare items..sending set operation to g0 master...
while [ 1 ]
do
   if [ $num -eq $item_count ]
   then
      break
   fi
   echo -e "set test$num 0 0 1\r\nn\r" | nc localhost $g0_m_port 1> /dev/null
   num=`expr $num + 1`;
done
echo end prepare items.

echo ">>>>>> sleep for $start_time before starting"
sleep $start_time

#run join and leave processing
while [ $COUNTER -le $run_count ];
do
   #join case
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join"
   echo cluster join begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
   echo cluster join | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration join"
   echo cluster join end | nc localhost $g3_m_port

   sleep 10
   print_item_count_some

   sleep 30
   echo "g4 M-$g4_m_port, S-$g4_s_port migration join"
   echo cluster join alone | nc localhost $g4_m_port
   echo "send all migration join command"

   sleep 1
   kill_exist
   sleep 60

   echo "restart and join"
   ./killandrun.memcached.mg.bash master $g2_m_port NONE
   ./killandrun.memcached.mg.bash slave $g2_s_port NONE

   sleep 5
   echo cluster join alone | nc localhost $g2_m_port

   sleep 3
   print_item_count

   sleep 30

   #leave case
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave"
   echo cluster leave begin | nc localhost $g1_m_port
   sleep 3
   echo "g4 M-$g4_m_port, S-$g4_s_port migration leave"
   echo cluster leave end | nc localhost $g4_m_port
   echo "send all migration leave command"

   sleep 1
   kill_exist
   sleep 2
   kill_exist_2
   sleep 60

   echo "restart failure node"
   ./killandrun.memcached.mg.bash master $g2_m_port NONE
   ./killandrun.memcached.mg.bash slave $g2_s_port NONE

   ./killandrun.memcached.mg.bash master $g3_m_port NONE
   ./killandrun.memcached.mg.bash slave $g3_s_port NONE

   sleep 3
   print_item_count

   if [ $g1_count -gt 10 ]
   then
      echo "$g1_m_port count is not 1"
      echo stats cachedump 0 10 | nc localhost $g1_m_port
      break
   fi

   if [ $g2_count -gt 10 ]
   then
      echo "$g2_m_port count is not 1"
      echo stats cachedump 0 10 | nc localhost $g2_m_port
      break
   fi

   if [ $g3_count -gt 10 ]
   then
      echo "$g3_m_port count is not 1"
      echo stats cachedump 0 10 | nc localhost $g3_m_port
      break
   fi

   if [ $g4_count -gt 10 ]
   then
      echo "$g4_m_port count is not 1"
      echo stats cachedump 0 10 | nc localhost $g4_m_port
      break
   fi

   sleep 30

   echo ">>>>>> sleep for $run_interval"
   sleep $run_interval
   echo ">>>>>> wakeup"

   let COUNTER=COUNTER+1
done
