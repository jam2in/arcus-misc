# Find os type. if system`s os is Mac OS X, we use greadlink.
case "$OSTYPE" in
  darwin*) DIR=`greadlink -f $0`;;
  *) DIR=`readlink -f $0`;;
esac

DIR=`dirname $DIR`

ZK_CLI="$DIR/../../../arcus/zookeeper/bin/zkCli.sh"
ZK_ADDR="-server localhost:9181"

if [ $# -eq 1 ]; then
    CLUSTER_IP=`ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print \$1}'`
    if [ $1 -eq 0 ]; then # 3nodeClusterIp
        N3_CLUSTER_IP=$CLUSTER_IP
    elif [ $1 -eq 1 ]; then # 4nodeClusterIp
        N4_CLUSTER_IP=$CLUSTER_IP
    else
        echo "Usage) ./integration/setup-test-zk-intg-idc.bash <cluster mode(0:3node, 1:4node)>"
        exit 1;
    fi
else
    echo "Usage) ./integration/setup-test-zk-intg-idc.bash <cluster mode(0:3node, 1:4node)>"
    exit 1;
fi

$ZK_CLI $ZK_ADDR rmr /arcus_repl 0
$ZK_CLI $ZK_ADDR create /arcus_repl 0

$ZK_CLI $ZK_ADDR create /arcus_repl/client_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/client_list/test_idc 0

$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_log 0

$ZK_CLI $ZK_ADDR create /arcus_repl/cache_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cache_list/test_idc 0

$ZK_CLI $ZK_ADDR create /arcus_repl/group_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g0 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g1 0
#$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g2 0

$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g3 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g4 0
$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g5 0
#$ZK_CLI $ZK_ADDR create /arcus_repl/group_list/test_idc/g6 0

$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping 0

# cluster A (3 node)
if [ $1 -eq 0 ]; then
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N3_CLUSTER_IP:11301 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N3_CLUSTER_IP:11301/test_idc^g0^$N3_CLUSTER_IP:20221^$N3_CLUSTER_IP:21221^$N3_CLUSTER_IP:22221^$N3_CLUSTER_IP:23221 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N3_CLUSTER_IP:11303 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N3_CLUSTER_IP:11303/test_idc^g1^$N3_CLUSTER_IP:20223^$N3_CLUSTER_IP:21223^$N3_CLUSTER_IP:22223^$N3_CLUSTER_IP:23223 0
    #$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N3_CLUSTER_IP:11305 0
    #$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N3_CLUSTER_IP:11305/test_idc^g2^$N3_CLUSTER_IP:20225^$N3_CLUSTER_IP:21225^$N3_CLUSTER_IP:22225^$N3_CLUSTER_IP:23225 0

    $ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$N3_CLUSTER_IP:11306 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$N3_CLUSTER_IP:11306/test_idc^$N3_CLUSTER_IP:24226^$N3_CLUSTER_IP:25226 0
fi


# cluster B (4 node)
if [ $1 -eq 1 ]; then
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11307 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11307/test_idc^g3^$N4_CLUSTER_IP:20227^$N4_CLUSTER_IP:21227^$N4_CLUSTER_IP:22227^$N4_CLUSTER_IP:23227 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11309 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11309/test_idc^g4^$N4_CLUSTER_IP:20229^$N4_CLUSTER_IP:21229^$N4_CLUSTER_IP:22229^$N4_CLUSTER_IP:23229 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11311 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11311/test_idc^g5^$N4_CLUSTER_IP:20231^$N4_CLUSTER_IP:21231^$N4_CLUSTER_IP:22231^$N4_CLUSTER_IP:23231 0
    #$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11313 0
    #$ZK_CLI $ZK_ADDR create /arcus_repl/cache_server_mapping/$N4_CLUSTER_IP:11313/test_idc^g6^$N4_CLUSTER_IP:20233^$N4_CLUSTER_IP:21233^$N4_CLUSTER_IP:22233^$N4_CLUSTER_IP:23233 0

    $ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$N4_CLUSTER_IP:11314 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$N4_CLUSTER_IP:11314/test_idc^$N4_CLUSTER_IP:24234^$N4_CLUSTER_IP:25234 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$N4_CLUSTER_IP:11316 0
    $ZK_CLI $ZK_ADDR create /arcus_repl/bridge_server_mapping/$N4_CLUSTER_IP:11316/test_idc^$N4_CLUSTER_IP:24236^$N4_CLUSTER_IP:25236 0
fi

$ZK_CLI $ZK_ADDR create /arcus_repl/xdcr_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/xdcr_list/test_idc 0

$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list/test_idc 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list/test_idc/stash_node 0
$ZK_CLI $ZK_ADDR create /arcus_repl/bridge_list/test_idc/xdcr_node 0

$ZK_CLI $ZK_ADDR create /arcus_repl/zkensemble_list 0
$ZK_CLI $ZK_ADDR create /arcus_repl/zkensemble_list/test_idc 0

$ZK_CLI $ZK_ADDR create /arcus_repl/cloud_stat 0
$ZK_CLI $ZK_ADDR create /arcus_repl/cloud_stat/test_idc 0
