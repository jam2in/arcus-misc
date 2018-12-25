#!/usr/bin/perl -w

use strict;

# use port 11301 ~ 11314, 21221 ~ 21234
my $keyset_size = 1000;
my $IDC_A_IP;
my $IDC_B_IP;
my $run_time = 200;
sub print_usage {
    print "Usage) perl ./integration/run_integration_idc.pl <IDC_A_IP> <IDC_B_IP>\n";
}

if ($#ARGV eq 1) {
    $IDC_A_IP = "$ARGV[0]";
    $IDC_B_IP = "$ARGV[1]";
    print "keyset_size = $keyset_size\n";
    print "IDC_A_IP = $IDC_A_IP\n";
    print "IDC_B_IP = $IDC_B_IP\n";
} else {
    print_usage();
    die;
}

my $cmd;
my $ret;

#########################################
# 1. set operation on IDC A
#########################################
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    "zookeeper=$IDC_A_IP:9181\n" .
    "service_code=test_idc\n" .
    "client=32\n" .
    "rate=15\n" .
    "request=0\n" .
    "time=$run_time\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=10\n" .
    "valueset_max_size=30\n" .
    "pool=2\n" .
    "pool_size=32\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "client_exptime=0\n" .
    "client_timeout=1000\n" .
    "client_profile=simple_set\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt &";
print "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
$ret = system($cmd);

sleep 5;

#########################################
# 2. set operation on IDC B
#########################################
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    "zookeeper=$IDC_B_IP:9181\n" .
    "service_code=test_idc\n" .
    "client=32\n" .
    "rate=10\n" .
    "request=0\n" .
    "time=$run_time\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=10\n" .
    "valueset_max_size=30\n" .
    "pool=2\n" .
    "pool_size=32\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "client_exptime=0\n" .
    "client_timeout=1000\n" .
    "client_profile=simple_set\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt &";
print "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
$ret = system($cmd);
if ($ret ne 0) {
  print "#########################\n";
  print "TEST FAILED CODE=$ret >> failed idc get operation\n";
  print "#########################\n";
  exit(1);
}

sleep 120; # after few seconds.. run kill command.

########################################################
# 3. kill acp-java same time 
########################################################
$cmd = "kill -9 `ps -ef | grep 'acp -config' | awk '{print \$2}'`";
print "RUN COMMAND = $cmd\n";
system($cmd);
print "arcus misc killed... start get operation\n";

sleep 3;

my $Adumpfile = "tmp-integration-dumpfile-AIDC.txt";
my $Bdumpfile = "tmp-integration-dumpfile-BIDC.txt";

$cmd = "rm ./$Adumpfile";
system($cmd);
$cmd = "rm ./$Bdumpfile";
system($cmd);
########################################################
# 4. get operation on A IDC
########################################################

open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    "zookeeper=$IDC_A_IP:9181\n" .
    "service_code=test_idc\n" .
    "client=32\n" .
    "rate=0\n" .
    "request=0\n" .
    "time=-1\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=10\n" .
    "valueset_max_size=30\n" .
    "pool=2\n" .
    "pool_size=32\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "operation_dumpfile=$Adumpfile\n" .
    "client_exptime=0\n" .
    "client_timeout=1000\n" .
    "client_profile=integration_idc_onlyget\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt &";
print "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
$ret = system($cmd);
if ($ret ne 0) {
  print "#########################\n";
  print "TEST FAILED CODE=$ret >> failed idc set operation\n";
  print "#########################\n";
  exit(1);
}

sleep 5;

########################################################
# 5. get operation on B IDC
########################################################
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    "zookeeper=$IDC_B_IP:9181\n" .
    "service_code=test_idc\n" .
    "client=32\n" .
    "rate=0\n" .
    "request=0\n" .
    "time=-1\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=10\n" .
    "valueset_max_size=30\n" .
    "pool=2\n" .
    "pool_size=32\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "operation_dumpfile=$Bdumpfile\n" .
    "client_exptime=0\n" .
    "client_timeout=1000\n" .
    "client_profile=integration_idc_onlyget\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt";
print "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
$ret = system($cmd);
if ($ret ne 0) {
  print "#########################\n";
  print "TEST FAILED CODE=$ret >> failed idc set operation\n";
  print "#########################\n";
  exit(1);
}

