#!/bin/bash

g0_m_port=11281
g0_s_port=11282
g1_m_port=11283
g1_s_port=11284
g2_m_port=11285
g2_s_port=11286
g3_m_port=11287
g3_s_port=11288
g4_m_port=11289
g4_s_port=11290

if [[ $# -le 2 && $# -ge 1 ]]; then
    MIGRATION="$1" # 0         : leave
                   # 1         : join
                   # 2 or more : stats
    if [ $# -eq 2 ]; then
        SERVER_IP="$2"
    else
        SERVER_IP="127.0.0.1"
    fi
else
    echo "Usage) ./integration/migration_start_manual.bash <0(leave) or 1(join) or 2(flush all nodes) or 3(node stats)> <SERVER_IP>"
    exit 1;
fi

####################################
############ function ##############
####################################

# stats
function g0_stats() {
   echo stats | nc $SERVER_IP $g0_m_port | grep curr_items
}

function g1_stats() {
   echo stats | nc $SERVER_IP $g1_m_port | grep curr_items
}

function g2_stats() {
   echo stats | nc $SERVER_IP $g2_m_port | grep curr_items
}

function g3_stats() {
   echo stats | nc $SERVER_IP $g3_m_port | grep curr_items
}

function g4_stats() {
    echo stats | nc $SERVER_IP $g4_m_port | grep curr_items
}

# migration state
function g0_migstats() {
   echo stats migration | nc $SERVER_IP $g0_m_port | grep $1
}

function g1_migstats() {
   echo stats migration | nc $SERVER_IP $g1_m_port | grep $1
}

function g2_migstats() {
   echo stats migration | nc $SERVER_IP $g2_m_port | grep $1
}

function g3_migstats() {
   echo stats migration | nc $SERVER_IP $g3_m_port | grep $1
}

function g4_migstats() {
   echo stats migration | nc $SERVER_IP $g4_m_port | grep $1
}

g0_count=0
g1_count=0
g2_count=0
g3_count=0
g4_count=0

function print_item_count() {
  tot=0
  echo ">>> $g0_m_port g0 items"
  g0_str=$(g0_stats)
  if [ -z "$g0_str" ]; then
    echo "g0 down"
  else
    g0_sub=${g0_str:16}
    g0_count=`echo $g0_sub | sed 's/[^0-9]//g'`
    echo $g0_str
    let tot+=${g0_str//[^0-9]/}
  fi

  echo ">>> $g1_m_port g1 items"
  g1_str=$(g1_stats)
  if [ -z "$g1_str" ]; then
    echo "g1 down"
  else
    g1_sub=${g1_str:16}
    g1_count=`echo $g1_sub | sed 's/[^0-9]//g'`
    echo $g1_str
    let tot+=${g1_str//[^0-9]/}
  fi

  echo ">>> $g2_m_port g2 items"
  g2_str=$(g2_stats)
  if [ -z "$g2_str" ]; then
    echo "g2 down"
  else
    g2_sub=${g2_str:16}
    g2_count=`echo $g2_sub | sed 's/[^0-9]//g'`
    echo $g2_str
    let tot+=${g2_str//[^0-9]/}
  fi

  echo ">>> $g3_m_port g3 items"
  g3_str=$(g3_stats)
  if [ -z "$g3_str" ]; then
    echo "g3 down"
  else
    g3_sub=${g3_str:16}
    g3_count=`echo $g3_sub | sed 's/[^0-9]//g'`
    echo $g3_str
    let tot+=${g3_str//[^0-9]/}
  fi

  echo ">>> $g4_m_port g4 items"
  g4_str=$(g4_stats)
  if [ -z "$g4_str" ]; then
    echo "g4 down"
  else
    g4_sub=${g4_str:16}
    g4_count=`echo $g4_sub | sed 's/[^0-9]//g'`
    echo $g4_str
    let tot+=${g4_str//[^0-9]/}
  fi
  echo -e "\\033[32mtotal item count : $tot\\033[0m"
}

function print_info() {
  if [[ "$2" == "UNKNOWN"* ]]; then
    echo -e "$1 $2 : \\033[32m$3\\033[0m"
  else
    echo -e "$1 $2 : \\033[33m$3\\033[0m"
  fi
}

function print_mig_state() {
  echo ">>> state of migration"
  g0_str=$(g0_migstats sm_info.mg_state)
  g0_state=${g0_str:22}
  g0_str=$(g0_migstats sm_info.mg_role)
  g0_role=${g0_str:21}

  g1_str=$(g1_migstats sm_info.mg_state)
  g1_state=${g1_str:22}
  g1_str=$(g1_migstats sm_info.mg_role)
  g1_role=${g1_str:21}

  g2_str=$(g2_migstats sm_info.mg_state)
  g2_state=${g2_str:22}
  g2_str=$(g2_migstats sm_info.mg_role)
  g2_role=${g2_str:21}

  g3_str=$(g3_migstats sm_info.mg_state)
  g3_state=${g3_str:22}
  g3_str=$(g3_migstats sm_info.mg_role)
  g3_role=${g3_str:21}

  g4_str=$(g4_migstats sm_info.mg_state)
  g4_state=${g4_str:22}
  g4_str=$(g4_migstats sm_info.mg_role)
  g4_role=${g4_str:21}

  print_info g0 state $g0_state
  print_info g1 state $g1_state
  print_info g2 state $g2_state
  print_info g3 state $g3_state
  print_info g4 state $g4_state
  echo
  print_info g0 role $g0_role
  print_info g1 role $g1_role
  print_info g2 role $g2_role
  print_info g3 role $g3_role
  print_info g4 role $g4_role
}


####################################
############ run test ##############
####################################

if [ $MIGRATION -eq 0 ]; then
  # leave
  echo "g3 M-$g3_m_port, S-$g3_s_port migration leave"
  response='';
  while [ "${response:0:2}" != "OK" ]
  do
  response=`echo cluster leave begin| nc $SERVER_IP $g3_m_port`
  sleep 1
  done

###########################################3
#  echo "g1 M-$g1_m_port, S-$g1_s_port migration leave"
#  response='';
#  while [ "${response:0:2}" != "OK" ]
#  do
#  response=`echo cluster leave | nc $SERVER_IP $g1_m_port`
#  sleep 1
#  done
#
#  echo "g2 M-$g2_m_port, S-$g2_s_port migration leave"
#  response='';
#  while [ "${response:0:2}" != "OK" ]
#  do
#  response=`echo cluster leave | nc $SERVER_IP $g2_m_port`
#  sleep 1
#  done
############################################

  echo "g4 M-$g4_m_port, S-$g4_s_port migration leave"
  response='';
  while [ "${response:0:2}" != "OK" ]
  do
  response=`echo cluster leave end | nc $SERVER_IP $g4_m_port`
  sleep 0.1
  done
  echo "send all migration leave command"
elif [ $MIGRATION -eq 1 ]; then
  # join
  echo "g3 M-$g3_m_port, S-$g3_s_port migration join"
  response='';
  while [ "${response:0:2}" != "OK" ]
  do
  response=`echo cluster join begin| nc $SERVER_IP $g3_m_port`
  sleep 1
  done

#############################################
#  echo "g1 M-$g1_m_port, S-$g1_s_port migration join"
#  response='';
#  while [ "${response:0:2}" != "OK" ]
#  do
#  response=`echo cluster join | nc $SERVER_IP $g1_m_port`
#  sleep 1
#  done
#
#  echo "g2 M-$g2_m_port, S-$g2_s_port migration join"
#  response='';
#  while [ "${response:0:2}" != "OK" ]
#  do
#  response=`echo cluster join | nc $SERVER_IP $g2_m_port`
#  sleep 1
#  done
########################################3

  echo "g4 M-$g4_m_port, S-$g4_s_port migration join"
  response='';
  while [ "${response:0:2}" != "OK" ]
  do
  response=`echo cluster join end | nc $SERVER_IP $g4_m_port`
  sleep 0.1
  done
  echo "send all migration join command"
elif [ $MIGRATION -eq 2 ]; then
  # flush all nodes
  echo flush_all | nc $SERVER_IP $g0_m_port
  echo scrub | nc $SERVER_IP $g0_m_port
  echo flush_all | nc $SERVER_IP $g1_m_port
  echo scrub | nc $SERVER_IP $g1_m_port
  echo flush_all | nc $SERVER_IP $g2_m_port
  echo scrub | nc $SERVER_IP $g2_m_port
  echo flush_all | nc $SERVER_IP $g3_m_port
  echo scrub | nc $SERVER_IP $g3_m_port
  echo flush_all | nc $SERVER_IP $g4_m_port
  echo scrub | nc $SERVER_IP $g4_m_port
  echo "send \"flush_al\", \"scrub\"l all nodes"
else
  while [ 1 ]
  do
    clear
    print_mig_state
    echo
    print_item_count
    sleep 1
  done
fi

