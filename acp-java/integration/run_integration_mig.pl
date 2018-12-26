#!/usr/bin/perl -w

use strict;
use Term::ANSIColor qw(:constants);

# use port 11281 ~ 11290, 21125 ~ 21134
my $t_ip   = "127.0.0.1";
my $t_port = "9181";
my $t_server;
my $flag = -1; # -1 : start test(client, server)
               #  0 : start test(only server)
               #  1 : start test(only client)
my $run_time = 600;
my $keyset_size = 10000000;
my $cmd;
my $ret;

sub print_usage {
    print "Usage) perl ./integration/run_integration_mig.pl [[server(0) client(1)] <SERVER_IP>]\n";
}

if ($#ARGV == 1 || $#ARGV == -1) {
    if ($#ARGV >= 0) {
        if ($ARGV[0] ) {
            $flag = 1;
        } else {
            $flag = 0;
        }
    }
    if ($#ARGV eq 1) {
        $t_ip = $ARGV[1];
    }
    print "runtime = $run_time\n";
    print "keyset_size = $keyset_size\n";
    print "t_ip = $t_ip\n";
    print "t_port = $t_port\n";
    print "flag = $flag\n";
} else {
    print_usage();
    die;
}
$t_server = $t_ip . ":" . $t_port;

use Cwd 'abs_path';
use File::Basename;

if ($flag eq -1 || $flag eq 0) {
    ###########################################
    ######### 1. group g0 node alone ##########
    ###########################################
    #$cmd = "./integration/run.memcached.bash 11281 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11282 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11283 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11284 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11285 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11286 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11287 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11288 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11289 sync $t_ip;"
    #        . "./integration/run.memcached.bash 11290 sync $t_ip";
    #system($cmd);
    $cmd = "./integration/run.memcached.bash 11281 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11282 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11283 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11284 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11285 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11286 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11287 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11288 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11289 sync $t_ip";
    system($cmd);
    $cmd = "./integration/run.memcached.bash 11290 sync $t_ip";
    system($cmd);
    print "11281, 11282, 11283, 11284, 11285, 11286, 11287, 11288, 11289, 11290 memcached node start";
    sleep(3);
    $cmd = "echo \"cluster join alone\" | nc localhost 11281"; system($cmd);
    print GREEN, "g0 M-11281, S-11282 migration join\n", RESET; sleep(3);
    $cmd = "echo \"cluster join begin\" | nc localhost 11283"; system($cmd);
    print GREEN, "g0 M-11283, S-11284 migration join\n", RESET; sleep(1);
    $cmd = "echo \"cluster join end\" | nc localhost 11285"; system($cmd);
    print GREEN, "g0 M-11285, S-11286 migration join\n", RESET; sleep(10);
    $ret = 0;
}

if ($flag eq -1 || $flag eq 1) {
    ###########################################
    ############# 2. insert data ##############
    ###########################################
    open CONF, ">tmp-integration-config.txt" or die $!;
    print CONF
        "zookeeper=$t_server\n" .
        "service_code=test_mg\n" .
        #"single_server=" . $t_ip . ":" . $t_port . "\n" .
        "client=30\n" .
        "rate=0\n" .
        "request=0\n" .
        "time=-1\n" .
        "keyset_size=$keyset_size\n" .
        "valueset_min_size=20\n" .
        "valueset_max_size=20\n" .
        "pool=1\n" .
        "pool_size=20\n" .
        "pool_use_random=true\n" .
        "key_prefix=integrationtest:\n" .
        "client_exptime=0\n" .
        "client_profile=integration_onlyset\n";
    close CONF;

    $cmd = "./run.bash -config tmp-integration-config.txt";
    printf "RUN COMMAND=%s\n", $cmd;

    local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
    $ret = system($cmd);

    if ($ret ne 0) {
      print RED, "#########################\n";
      print "TEST FAILED CODE=$ret >> migration insert data\n";
      print "#########################\n", RESET;
      exit(1);
    }

    ###########################################
    ############### 3. get data ###############
    ###########################################
    open CONF, ">tmp-integration-config.txt" or die $!;
    print CONF
        "zookeeper=$t_server\n" .
        "service_code=test_mg\n" .
        #"single_server=" . $t_ip . ":" . $t_port . "\n" .
        "client=20\n" .
        "rate=0\n" .
        "request=0\n" .
        "time=$run_time\n" .
        "keyset_size=$keyset_size\n" .
        "valueset_min_size=20\n" .
        "valueset_max_size=20\n" .
        "pool=1\n" .
        "pool_size=20\n" .
        "pool_use_random=true\n" .
        "key_prefix=integrationtest:\n" .
        "client_exptime=0\n" .
        "client_profile=integration_onlyget\n";
    close CONF;
    $cmd = "./run.bash -config tmp-integration-config.txt";
    printf "RUN COMMAND=%s\n", $cmd;

    local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
    $ret = system($cmd);

    if ($ret ne 0) {
      print RED, "#########################\n";
      print "TEST FAILED CODE=$ret >> switchover get data\n";
      print "#########################\n", RESET;
      exit(1);
    }

    ###########################################
    ############## 4. flush data ##############
    ###########################################
#    $cmd = "./integration/migration_start_manual.bash 2 $t_ip"; # flush_all and scrub all nodes
#    system($cmd);
#    print GREEN, "flush_all and scrub all nodes sleep 15 sec\n", RESET;
#    sleep 15;

    ###########################################
    ########## 5. join/leave start ############
    ###########################################
    print GREEN, "join/leave start\n", RESET;
    $cmd = "touch __can_migtest_failure__";
    system($cmd);
    sleep 1;
    $cmd = "./integration/migration_start_auto.bash $t_ip &";
    system($cmd);
    sleep 5;

    ###########################################
    ############## 6. intg test ###############
    ###########################################
    open CONF, ">tmp-integration-config.txt" or die $!;
    print CONF
        "zookeeper=$t_server\n" .
        "service_code=test_mg\n" .
        #"single_server=" . $t_ip . ":" . $t_port . "\n" .
        "client=30\n" .
        "rate=500\n" .
        "request=0\n" .
        "time=$run_time\n" .
        "keyset_size=$keyset_size\n" .
        "valueset_min_size=20\n" .
        "valueset_max_size=20\n" .
        "pool=1\n" .
        "pool_size=20\n" .
        "pool_use_random=true\n" .
        "key_prefix=integrationtest:\n" .
        "client_exptime=0\n" .
        "client_profile=integration_arcus\n";
    close CONF;
    $cmd = "./run.bash -config tmp-integration-config.txt";
    printf "RUN COMMAND=%s\n", $cmd;

    local $SIG{TERM} = sub { print "TERM SIGNAL\n" };
    $ret = system($cmd);

    $cmd = "rm -rf __can_migtest_failure__";
    system($cmd);
}

if ($ret ne 0) {
  print RED, "#########################\n";
  print "TEST FAILED CODE=$ret >> integration test in migration\n";
  print "#########################\n", RESET;
  exit(1);
} else {
  print GREEN, "#########################\n";
  print "SUCCESS MIGRATION TEST\n";
  print "#########################\n", RESET;
}
