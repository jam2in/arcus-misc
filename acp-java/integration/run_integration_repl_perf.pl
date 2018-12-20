#!/usr/bin/perl -w

use strict;

my $m_port = 11291; # master port
my $s_port = 11292; # slave  port
my $zk_ip; # zookeeper ip
my $mode;  # "sync" or "async"
my $run_time = 600;
my $keyset_size = 10000000;
my $expect_perf = 0;
my $flag = -1; # -1 : start test(client, server)
               #  0 : start test(only server)
               #  1 : start test(only client)
my $cmd;

sub print_usage {
  print "Usage) perl ./integration/run_integration_repl_perf.pl <mode> <flag> <ZK_IP>\n";
  print "   <mode>  : replication mode, sync or async\n";
  print "   <flag>  : master run - 0, slave & client run - 1\n";
  print "   <ZK_IP> : zookeeper ip address\n";
}

if ($#ARGV == 0 || $#ARGV == 1 || $#ARGV == 2) {
  if ($#ARGV >= 1) {
    $flag = $ARGV[1];
  }

  $zk_ip = `ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print \$1}'`;
  $zk_ip =~ s/\n//g;

  if ($#ARGV == 2 && $flag == 1) {
      $zk_ip = $ARGV[2];
  }

  if (($ARGV[0] eq "sync") || ($ARGV[0] eq "async")) {
    $mode = $ARGV[0];
    if ($ARGV[0] eq "async") {
      $expect_perf = 80000; #async perf
    } else {
      $expect_perf = 40000; #sync perf
    }
  } else {
      print_usage();
      die;
  }

  print "zk_ip = $zk_ip\n";
  print "master_port = $m_port\n";
  print "slave_port  = $s_port\n";
  print "run_time = $run_time\n";
  print "keyset_size = $keyset_size\n";
  print "repl_mode = $mode\n";
  print "performance = $expect_perf\n";
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
  $cmd = "./integration/run.memcached.bash $s_port $mode $zk_ip";
  system($cmd);
} elsif ($flag eq 0) {
  # master node
  $cmd = "./integration/run.memcached.bash $m_port $mode $zk_ip";
  system($cmd);
} elsif ($flag eq 1) {
  # slave node
  $cmd = "./integration/run.memcached.bash $s_port $mode $zk_ip";
  system($cmd);
  sleep 1;
  $cmd = "echo \"cluster join alone\" | nc $zk_ip $m_port"; system($cmd);
}
sleep 3;

########################################
# 2. run integration perf
########################################
if ($flag eq -1 || $flag eq 1) {
  if ($mode eq "sync") {
      $cmd = "./integration/run_integration_sync_perf.pl $zk_ip $m_port $expect_perf";
  } else {
      $cmd = "./integration/run_integration_res_perf.pl $zk_ip $m_port $expect_perf";
  }
  system($cmd);
  sleep 2;
}

########################################
# 3. node kill
########################################
if ($flag eq -1) {
  $cmd = "kill \$(ps -ef | awk '/sync.config; -p $m_port/ {print \$2}')";
  printf "RUN COMMAND = $cmd\n";
  printf "kill master node($m_port)\n";
  system($cmd);
  $cmd = "kill \$(ps -ef | awk '/sync.config; -p $s_port/ {print \$2}')";
  printf "RUN COMMAND = $cmd\n";
  printf "kill slave node($s_port)\n";
  system($cmd);
}

print "############################\n";
print "# END REPLICATION PERF TEST\n";
print "############################\n";
