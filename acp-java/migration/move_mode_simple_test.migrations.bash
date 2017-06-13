#!/bin/bash

join_num=0;
leave_num=0;
mode=1;
mode_str=("global" "random")

touch random_simple.migration.log

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
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join ${mode_str[$mode]}"
   echo cluster join alone ${mode_str[$mode]} | nc localhost $g1_m_port
   echo "send all migration join command"
}

function jcase2(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join ${mode_str[$mode]}"
   echo cluster join begin ${mode_str[$mode]} | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
   echo cluster join end | nc localhost $g2_m_port
   echo "send all migration join command"
}

function jcase3(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join ${mode_str[$mode]}"
   echo cluster join begin ${mode_str[$mode]} | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
   echo cluster join | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration join"
   echo cluster join end | nc localhost $g3_m_port
   echo "send all migration join command"
}

function jcase4(){
   echo "Migration join count: $join_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration join ${mode_str[$mode]}"
   echo cluster join begin ${mode_str[$mode]} | nc localhost $g1_m_port
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
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave ${mode_str[$mode]}"
   echo cluster leave alone ${mode_str[$mode]} | nc localhost $g1_m_port
   echo "send all migration join command"
}

function lcase2(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave ${mode_str[$mode]}"
   echo cluster leave begin ${mode_str[$mode]} | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
   echo cluster leave end | nc localhost $g2_m_port
   echo "send all migration join command"
}

function lcase3(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave ${mode_str[$mode]}"
   echo cluster leave begin ${mode_str[$mode]} | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
   echo cluster leave | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration leave"
   echo cluster leave end | nc localhost $g3_m_port
   echo "send all migration join command"
}

function lcase4(){
   echo "Migration leave count: $leave_num"
   echo "g1 M-$g1_m_port, S-$g1_s_port migration leave ${mode_str[$mode]}"
   echo cluster leave begin ${mode_str[$mode]} | nc localhost $g1_m_port
   sleep 3
   echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
   echo cluster leave | nc localhost $g2_m_port
   echo "g3 M-$g3_m_port, S-$g3_s_port migration leave"
   echo cluster leave | nc localhost $g3_m_port
   echo "g4 M-$g4_m_port, S-$g4_s_port migration leave"
   echo cluster leave end | nc localhost $g4_m_port
   echo "send all migration join command"
}

echo "all migration node run"
./start_memcached_migration.bash

echo "g0 M-$g0_m_port, S-$g0_s_port migration join"
echo cluster join alone | nc localhost $g0_m_port

sleep 5

while [ 1 ]
do
   mode=$(($RANDOM%2))
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

   mode=$(($RANDOM%2))
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
done
