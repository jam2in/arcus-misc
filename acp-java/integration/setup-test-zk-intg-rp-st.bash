# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`

ZK_CLI="$DIR/../../../arcus/zookeeper/bin/zkCli.sh"
ZK_ADDR="-server localhost:9181"

if [ $# -eq 1 ]; then
  M_SERVER_IP="$1"
  S_SERVER_IP="$1"
elif [ $# -eq 2 ]; then
  M_SERVER_IP="$1" # master node ip
  S_SERVER_IP="$2" # slave node ip
else
  echo "Usage) ./integration/setup-test-zk-intg-rp.bash <M_SERVER_IP> [S_SERVER_IP]"
  exit 1
fi

$ZK_CLI $ZK_ADDR rmr /arcus_repl 0
$ZK_CLI $ZK_ADDR create /arcus_repl 0

$ZK_CLI $ZK_ADDR create /arcus_repl/client_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/client_list/test_rp 0

$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_log 0

$ZK_CLI $ZK_ADDR create /arcus_repl/cache_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_list/test_rp 0

$ZK_CLI $ZK_ADDR create /arcus_repl/group_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_rp 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_rp/g0 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_rp/g1 0 #for switchover

# for functional & performance test
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$M_SERVER_IP:11291 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$M_SERVER_IP:11291/test_rp^g0^$M_SERVER_IP:20121^$M_SERVER_IP:20131^$M_SERVER_IP:20141^$M_SERVER_IP:20151 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$S_SERVER_IP:11292 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$S_SERVER_IP:11292/test_rp^g0^$S_SERVER_IP:20122^$S_SERVER_IP:20132^$S_SERVER_IP:20142^$S_SERVER_IP:20152 0

# for switchover test only use M_SERVER
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$M_SERVER_IP:11293 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$M_SERVER_IP:11293/test_rp^g1^$M_SERVER_IP:20123^$M_SERVER_IP:20133^$M_SERVER_IP:20143^$M_SERVER_IP:20153 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$M_SERVER_IP:11294 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$M_SERVER_IP:11294/test_rp^g1^$M_SERVER_IP:20124^$M_SERVER_IP:20134^$M_SERVER_IP:20144^$M_SERVER_IP:20154 0

# for stash node test
$ZK_CLI $ZK_ADDR create /arcus_repl/xdcr_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/xdcr_list/test_rp 0

$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list/test_rp 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list/test_rp/stash_node 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list/test_rp/xdcr_node 0

$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$M_SERVER_IP:11299 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$M_SERVER_IP:11299/test_rp^$M_SERVER_IP:11295^$M_SERVER_IP:11296 0

# for enable migration
$ZK_CLI $ZK_ADDR rmr /arcus_repl/cloud_stat 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cloud_stat 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cloud_stat/test_rp 0
