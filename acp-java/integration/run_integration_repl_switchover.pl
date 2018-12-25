#!/usr/bin/perl -w

use strict;

my $m_port = 11293; # master port
my $s_port = 11294; # slave  port
my $run_time = 600;
my $keyset_size = 10000000;
my $zk_ip = 127.0.0.1;
my $cmd = "";
sub print_usage {
  print "Usage) perl ./integration/run_integration_repl_switchover.pl <server_ip>\n";
}

if ($#ARGV eq 0) {
  $zk_ip = $ARGV[0];
  print "server_ip = $zk_ip\n";
  print "master_port = $m_port\n";
  print "slave_port  = $s_port\n";
  print "run_time = $run_time\n";
  print "keyset_size = $keyset_size\n";
} else {
  print_usage();
  die;
}

use Cwd 'abs_path';
use File::Basename;

########################################
# 1. start node(znode must be created) #
########################################
$cmd = "./integration/run.memcached.bash $m_port sync $zk_ip"; system($cmd);
$cmd = "./integration/run.memcached.bash $s_port sync $zk_ip"; system($cmd);
sleep 1;
$cmd = "echo \"cluster join alone\" | nc $zk_ip $m_port"; system($cmd);
sleep 1;

########################################
###### 2. start switchover daemon ######
########################################
my $run_interval = 30;
$cmd = "touch __can_test_failure__";
system($cmd);
print "switchover daemon start....\n";
$cmd = "./integration/loop.switchover.bash $m_port $s_port 0 $run_interval $run_time &";
system($cmd);




########################################
######## 3. get/set operation  #########
########################################
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    "zookeeper=127.0.0.1:9181\n" .
    "service_code=test_rp\n" .
    #"single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=10\n" .
    "rate=1000\n" .
    "request=0\n" .
    "time=600\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=10\n" .
    "valueset_max_size=30\n" .
    "pool=5\n" .
    "pool_size=30\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "client_exptime=0\n" .
    "client_timeout=3000\n" .
    "client_profile=integration_repltest\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
my $ret = system($cmd);
$cmd = "rm -f __can_test_failure__";
system($cmd);
if ($ret ne 0) {
  print "#########################\n";
  print "TEST FAILED CODE=$ret >> switchover get/set data\n";
  print "#########################\n";
  exit(1);
}

########################################
######## 4. master, slave kill #########
########################################

#$cmd = "kill \$(ps -ef | awk '/sync.config; -p $m_port/ {print \$2}')";
#printf "RUN COMMAND = $cmd\n";
#printf "master node($m_port) kill\n";
#system($cmd);
#$cmd = "kill \$(ps -ef | awk '/sync.config; -p $s_port/ {print \$2}')";
#printf "RUN COMMAND = $cmd\n";
#printf "slave node($s_port) kill\n";
#system($cmd);

print "#########################\n";
print "SUCCESS SWITCHOVER TEST\n";
print "#########################\n";
