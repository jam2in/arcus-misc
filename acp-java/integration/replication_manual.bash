#!/bin/bash

rp_m_port=11291
rp_s_port=11292

if [[ $# -le 3 && $# -ge 1 ]]; then
    RUN_FLAG="$1" # 0 : show replication mode
    if [ $# -le 2 ]; then
        MASTER_IP="$2"
    else
        MASTER_IP="127.0.0.1"
    fi

    if [ $# -le 3 ]; then
        MASTER_IP="$2"
        SLAVE_IP="$3"
    else
        SLAVE_IP="127.0.0.1"
    fi
else
    echo "Usage) ./integration/replication_manual.bash <0(status) MASTER_IP SLAVE_IP> <1(kill master)>";
    exit 1;
fi

####################################
############ function ##############
####################################

# stats
function m_stats() {
   echo stats replication | nc $MASTER_IP $rp_m_port | grep $1
}

function s_stats() {
   echo stats replication | nc $SLAVE_IP $rp_s_port | grep $1
}

function print_info() {
  if [[ "$2" == "UNKNOWN"* ]]; then
    echo -e "$1 $2 : \\033[32m$3\\033[0m"
  else
    echo -e "$1 $2 : \\033[33m$3\\033[0m"
  fi
}

function print_mig_state() {
  echo ">>> state of replication"
  m_str=$(m_stats mode)
  m_mode=${m_str:10}

  m_str=$(m_stats state)
  m_state=${m_str:11}

  s_str=$(s_stats mode)
  s_mode=${s_str:10}

  s_str=$(s_stats state)
  s_state=${s_str:11}

  echo "# master node"
  print_info $MASTER_IP\:$rp_m_port mode $m_mode
  print_info $MASTER_IP\:$rp_m_port state $m_state
  echo
  echo "# slave node"
  print_info $SLAVE_IP\:$rp_s_port mode $s_mode
  print_info $SLAVE_IP\:$rp_s_port state $s_state
}


####################################
############ run test ##############
####################################

if [ $RUN_FLAG -eq 0 ]; then
  while [ 1 ]
  do
    clear
    print_mig_state
    sleep 1
  done
elif [ $RUN_FLAG -eq 1 ]; then
  cmd=`perl ./integration/kill.memcached.perl 11291 0`
fi

