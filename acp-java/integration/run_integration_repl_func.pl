#!/usr/bin/perl -w

use strict;

my $m_port = 11291; # master port
my $s_port = 11292; # slave  port
my $stash_port = 11299;  # stash port
my $zk_ip = "127.0.0.1"; # server ip
my $mode = "sync";       # run as "sync" mode
my $run_time = 600;
my $keyset_size = 10000000;
my $flag = -1; # -1 : start test(client, server)
               #  0 : start test(only server)
               #  1 : start test(only client)
my $cmd;

sub print_usage {
  print "Usage) perl ./integration/run_integration_repl_func.pl <sync | async | stash> [[server(0) client(1)] [ZK_IP]]\n";
}

if (($#ARGV >= 0) & ($#ARGV <= 2)) {
  if ($#ARGV >= 1) {
    $flag = $ARGV[1];

    $zk_ip = `ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print \$1}'`;
    $zk_ip =~ s/\n//g;

    if ($#ARGV == 2 && $flag == 1) {
      $zk_ip = $ARGV[2];
    }
  }

  if (($ARGV[0] eq "sync") || ($ARGV[0] eq "async") || ($ARGV[0] eq "stash")) {
    $mode = $ARGV[0];
  } else {
    print_usage();
    die;
  }
  print "zookeeper_ip = $zk_ip\n";
  print "master_port = $m_port\n";
  print "slave_port  = $s_port\n";
  print "run_time = $run_time\n";
  print "keyset_size = $keyset_size\n";
  print "repl_mode = $mode\n";
} else {
  print_usage();
  die;
}

use Cwd 'abs_path';
use File::Basename;

########################################
# 1. start node(znode must be created)
########################################
if ($flag eq -1) {
  $cmd = "./integration/run.memcached.bash $m_port $mode $zk_ip";
  system($cmd);
  $cmd = "./integration/run.memcached.stash.bash $stash_port $mode $zk_ip";
  system($cmd);
  $cmd = "./integration/run.memcached.bash $s_port $mode $zk_ip";
  system($cmd);
} elsif ($flag eq 0) {
  # master node
  if ($mode eq "stash") {
    $cmd = "./integration/run.memcached.bash $m_port sync $zk_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.stash.bash $stash_port $mode $zk_ip";
    system($cmd);
  } else {
    $cmd = "./integration/run.memcached.bash $m_port $mode $zk_ip";
    system($cmd);
  }
} elsif ($flag eq 1) {
  # slave node
  $cmd = "./integration/run.memcached.bash $s_port $mode $zk_ip";
  system($cmd);
  $cmd = "echo \"cluster join alone\" | nc $zk_ip $m_port"; system($cmd);
}
sleep 3;

#############################################
# 3. after a few seconds master node down
#############################################
#if ($flag eq -1 || $flag eq 0) {
#  my $kill_time = 50; # after kill_time seconds. kill -9 master node
#  $cmd = "perl ./integration/kill.memcached.perl $m_port $kill_time &";
#  print "after $kill_time seconds... kill -9 master node\n";
#  print $cmd , "\n";
#  system($cmd);
#  sleep 1;
#}

##########################
# 4. enable stash node
##########################
if (($flag eq -1 || $flag eq 0) && ($mode eq "stash")) {
  $cmd = `echo "stash register g0" | nc localhost $stash_port`;
}

############################################
# 5. bandwidth limit start(master node)
############################################
#if ($flag eq -1 || $flag eq 0) {
#  $cmd = "./integration/bandwidth_limit.sh start $m_port";
#  system($cmd);
#  sleep 1;
#}

########################################
# 6. get/set operation
########################################
if ($flag eq -1 || $flag eq 1) {
  open CONF, ">tmp-integration-config.txt" or die $!;
  print CONF
      "zookeeper=$zk_ip:9181\n" .
      "service_code=test_rp\n" .
      #"single_server=" . $zk_ip . ":" . $t_port . "\n" .
      "client=100\n" .
      "rate=400\n" .
      "request=0\n" .
      "time=-1\n" .
      "keyset_size=$keyset_size\n" .
      "valueset_min_size=10\n" .
      "valueset_max_size=30\n" .
      "pool=5\n" .
      "pool_size=30\n" .
      "pool_use_random=false\n" .
      "key_prefix=integrationtest:\n" .
      "client_exptime=0\n" .
      "client_timeout=3000\n" .
      "client_profile=integration_repl_onlyset\n";
  close CONF;

  $cmd = "./run.bash -config tmp-integration-config.txt";
  printf "RUN COMMAND=%s\n", $cmd;
  local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
  my $ret = system($cmd);
  if ($ret ne 0) {
    print "#########################\n";
    print "TEST FAILED CODE=$ret >> replication operation in master node\n";
    print "#########################\n";
    exit(1);
  }

  sleep 5;

  open CONF, ">tmp-integration-config.txt" or die $!;
  print CONF
      "zookeeper=$zk_ip:9181\n" .
      "service_code=test_rp\n" .
      #"single_server=" . $zk_ip . ":" . $t_port . "\n" .
      "client=100\n" .
      "rate=400\n" .
      "request=0\n" .
      "time=-1\n" .
      "keyset_size=$keyset_size\n" .
      "valueset_min_size=10\n" .
      "valueset_max_size=30\n" .
      "pool=5\n" .
      "pool_size=30\n" .
      "pool_use_random=false\n" .
      "key_prefix=integrationtest:\n" .
      "client_exptime=0\n" .
      "client_timeout=3000\n" .
      "client_profile=integration_repl_onlyget\n";
  close CONF;

  $cmd = "./run.bash -config tmp-integration-config.txt";
  printf "RUN COMMAND=%s\n", $cmd;
  local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
  my $ret = system($cmd);

  if ($ret ne 0) {
    print "#########################\n";
    print "TEST FAILED CODE=$ret >> replication operation in master node\n";
    print "#########################\n";
    exit(1);
  }
}

######################################
# 7. bandwidth limit stop(master node)
######################################
#if ($flag eq -1) {
#  $cmd = "./integration/bandwidth_limit.sh stop $m_port";
#  system($cmd);
#  sleep 1;
#}

########################################
# 8. slave kill
########################################
if ($flag eq -1) {
  $cmd = "kill \$(ps -ef | awk '/sync.config; -p $s_port/ {print \$2}')";
  printf "RUN COMMAND = $cmd\n";
  printf "kill slave node($s_port)\n";
  system($cmd);
}

print "#########################\n";
print "SUCCESS REPLICATION TEST\n";
print "#########################\n";
