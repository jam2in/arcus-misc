#!/usr/bin/perl -w

use strict;

# use port 11301 ~ 11314, 21221 ~ 21234
my $run_time = 600;
my $keyset_size = 10000000;
my $mode = ""; # 0 : run cluster with 3 nodes.
               # 1 : run cluster with 4 nodes.
               # 2 : set operation on 3 nodes.
               # 3 : get operation on 4 nodes.
my $another_idc_ip = "";
sub print_usage {
    print "Usage) perl ./integration/run_integration_idc.pl <mode(0(3node start) | 1(4node start)) | 2(set operation(3node)) | 3(get operation(4node))> <another idc ip>\n";
}

if ($#ARGV eq 1 || $#ARGV eq 0) {
    $mode = $ARGV[0];
    if ($ARGV[0] == 0 || $ARGV[0] == 1) {
        if ($#ARGV eq 1) {
            $another_idc_ip = "$ARGV[1]" . ":9181";
        } else {
            print_usage();
            die;
        }
    }
    print "runtime = $run_time\n";
    print "keyset_size = $keyset_size\n";
    print "mode = $mode\n";
} else {
    print_usage();
    die;
}

my $cmd;

###########################################
# 1. start cluster
###########################################
if ($mode == 0) { # 3 nodes
    $cmd = "./integration/run.memcached.bash 11301;"
         . "./integration/run.memcached.bash 11303;";
    system($cmd);
    $cmd = "echo \"cluster join begin\" | nc localhost 11301";
    system($cmd); sleep(3);
    $cmd = "echo \"cluster join end\" | nc localhost 11303";
    system($cmd); sleep(3);
} elsif ($mode == 1) { # 4 nodes
    $cmd = "./integration/run.memcached.bash 11307;"
         . "./integration/run.memcached.bash 11309;"
         . "./integration/run.memcached.bash 11311;";
    system($cmd);
    $cmd = "echo \"cluster join begin\" | nc localhost 11307";
    system($cmd); sleep(3);
    $cmd = "echo \"cluster join\" | nc localhost 11309";
    system($cmd); sleep(3);
    $cmd = "echo \"cluster join end\" | nc localhost 11311";
    system($cmd); sleep(3);
}
#########################################
# 2. start xdcr node
#########################################
if ($mode == 0) { # 3 nodes
    $another_idc_ip = "10.32.24.105:9181";
    $cmd = "./integration/run.memcached.stash.bash 11306 $mode";
    system($cmd);
    $cmd = "echo \"xdcr register g0 $another_idc_ip\" | nc localhost 11306;"
         . "echo \"xdcr register g1 $another_idc_ip\" | nc localhost 11306;";
    system($cmd); sleep(1);
} elsif ($mode == 1) { # 4 nodes
    $another_idc_ip = "10.32.27.100:9181";
    $cmd = "./integration/run.memcached.stash.bash 11314 $mode;"
         . "./integration/run.memcached.stash.bash 11316 $mode;";
    system($cmd); sleep(1);
    $cmd = "echo \"xdcr register g3 $another_idc_ip\" | nc localhost 11314;"
         . "echo \"xdcr register g4 $another_idc_ip\" | nc localhost 11314;";
    system($cmd); sleep(1);
    $cmd = "echo \"xdcr register g5 $another_idc_ip\" | nc localhost 11316;";
    system($cmd); sleep(1);
}


#########################################
# 3. set operation on 3 nodes cluster
#########################################
if ($mode == 2) {
  my $operation_dumpfile = "tmp-integration-dumpfileA.txt";
  $cmd = "rm ./$operation_dumpfile";
  system($cmd);

  open CONF, ">tmp-integration-config.txt" or die $!;
  print CONF
      "zookeeper=10.32.27.100:9181\n" .
      "service_code=test_idc\n" .
      #"single_server=" . $zk_ip . ":" . $t_port . "\n" .
      "client=10\n" .
      "rate=0\n" .
      "request=0\n" .
      "time=$run_time\n" .
      "keyset_size=$keyset_size\n" .
      "valueset_min_size=10\n" .
      "valueset_max_size=30\n" .
      "pool=5\n" .
      "pool_size=30\n" .
      "pool_use_random=false\n" .
      "key_prefix=integrationtest:\n" .
      "operation_dumpfile=" . $operation_dumpfile . "\n" .
      "client_exptime=0\n" .
      "client_timeout=1000\n" .
      "client_profile=integration_idc_onlyset\n";
  close CONF;

  $cmd = "./run.bash -config tmp-integration-config.txt";
  printf "RUN COMMAND=%s\n", $cmd;

  local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
  my $ret = system($cmd);
  if ($ret ne 0) {
    print "#########################\n";
    print "TEST FAILED CODE=$ret >> failed idc set operation\n";
    print "#########################\n";
    exit(1);
  }
}

#########################################
# 4. get operation on 4 nodes cluster
#########################################
if ($mode == 3) {
  my $operation_dumpfile = "tmp-integration-dumpfileB.txt";
  $cmd = "rm ./$operation_dumpfile";
  system($cmd);

  open CONF, ">tmp-integration-config.txt" or die $!;
  print CONF
      "zookeeper=10.32.24.105:9181\n" .
      "service_code=test_idc\n" .
      #"single_server=" . $zk_ip . ":" . $t_port . "\n" .
      "client=100\n" .
      "rate=0\n" .
      "request=0\n" .
      "time=$run_time\n" .
      "keyset_size=$keyset_size\n" .
      "valueset_min_size=10\n" .
      "valueset_max_size=30\n" .
      "pool=5\n" .
      "pool_size=30\n" .
      "pool_use_random=false\n" .
      "key_prefix=integrationtest:\n" .
      "operation_dumpfile=" . $operation_dumpfile . "\n" .
      "client_exptime=0\n" .
      "client_timeout=1000\n" .
      "client_profile=integration_idc_onlyget\n";
  close CONF;

  $cmd = "./run.bash -config tmp-integration-config.txt";
  printf "RUN COMMAND=%s\n", $cmd;

  local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
  my $ret = system($cmd);
  if ($ret ne 0) {
    print "#########################\n";
    print "TEST FAILED CODE=$ret >> failed idc get operation\n";
    print "#########################\n";
    exit(1);
  }
}

print "#########################\n";
print "START IDC CLUSTER $mode\n";
print "#########################\n";
