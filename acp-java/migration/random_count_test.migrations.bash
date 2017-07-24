#!/bin/bash

join_num=0;
leave_num=0;
item_count=5000;

touch random_count.migration.log

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

function jcase1(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join"
   echo cluster join alone | nc localhost $g1_m_port
   echo "send all migration join command"
}

function jcase2(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join"
   echo cluster join begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
   echo cluster join end | nc localhost $g2_m_port
   echo "send all migration join command"
}

function jcase3(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join"
   echo cluster join begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
   echo cluster join | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration join"
   echo cluster join end | nc localhost $g3_m_port
   echo "send all migration join command"
}

function jcase4(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join"
   echo cluster join begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
   echo cluster join | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration join"
   echo cluster join | nc localhost $g3_m_port
   echo "g4 M-$g4_m_port, S-$g4_s_port migration join"
   echo cluster join end | nc localhost $g4_m_port
   echo "send all migration join command"
}

function lcase1(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave"
   echo cluster leave alone | nc localhost $g1_m_port
   echo "send all migration leave command"
}

function lcase2(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave"
   echo cluster leave begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
   echo cluster leave end | nc localhost $g2_m_port
   echo "send all migration leave command"
}

function lcase3(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave"
   echo cluster leave begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
   echo cluster leave | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration leave"
   echo cluster leave end | nc localhost $g3_m_port
   echo "send all migration leave command"
}

function lcase4(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave"
   echo cluster leave begin | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
   echo cluster leave | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration leave"
   echo cluster leave | nc localhost $g3_m_port
   echo "g4 M-$g4_m_port, S-$g4_s_port migration leave"
   echo cluster leave end | nc localhost $g4_m_port
   echo "send all migration leave command"
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

echo "all migration node run"
./start_memcached_migration.bash

echo "g0 M-$g0_m_port, S-$g0_s_port migration join"
echo cluster join alone | nc localhost $g0_m_port

sleep 5

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

sleep 5

while [ 1 ]
do
   jcase_num=$(($join_num%4))
   case "$jcase_num" in
   "0")
      jcase1
      ;;
   "1")
      jcase2
      ;;
   "2")
      jcase3
      ;;
   "3")
      jcase4
      ;;
   "*")
      echo "ERROR: too many join case"
      exit
      ;;
   esac
   join_num=`expr $join_num + 1`;

   sleep 30

   g0_str=$(g0_stats)
   g1_str=$(g1_stats)
   g2_str=$(g2_stats)
   g3_str=$(g3_stats)
   g4_str=$(g4_stats)

   g0_sub=${g0_str:16}
   g1_sub=${g1_str:16}
   g2_sub=${g2_str:16}
   g3_sub=${g3_str:16}
   g4_sub=${g4_str:16}

   g0_count=`echo $g0_sub | sed 's/[^0-9]//g'`
   g1_count=`echo $g1_sub | sed 's/[^0-9]//g'`
   g2_count=`echo $g2_sub | sed 's/[^0-9]//g'`
   g3_count=`echo $g3_sub | sed 's/[^0-9]//g'`
   g4_count=`echo $g4_sub | sed 's/[^0-9]//g'`

   sum=`expr $g0_count + $g1_count + $g2_count + $g3_count + $g4_count - 5`;
   if [ $sum -ne $item_count ]
   then
      echo ERROR: items count miss, join operation. expected = $item_count, now = $sum
      break
   else
      echo SUCCESS: all node items count is complete.
   fi

   sleep 30

   lcase_num=$(($leave_num%4))
   case "$lcase_num" in
   "0")
      lcase1
      ;;
   "1")
      lcase2
      ;;
   "2")
      lcase3
      ;;
   "3")
      lcase4
      ;;
   "*")
      echo "ERROR: too many leave case"
      exit
      ;;
   esac
   leave_num=`expr $leave_num + 1`;

   sleep 30

   g0_str=$(g0_stats)
   g0_sub=${g0_str:16}
   g0_count=`echo $g0_sub | sed 's/[^0-9]//g'`

   sum=`expr $g0_count - 1`;
   if [ $sum -ne $item_count ]
   then
      echo ERROR: items count miss, leave operation. expected = $item_count, now = $sum
      break
   else
      echo SUCCESS: all node items count is complete.
   fi

   sleep 30
done
