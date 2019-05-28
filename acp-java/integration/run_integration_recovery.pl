#!/usr/bin/perl -w
$t_ip = "127.0.0.1";   # test ip
$t_port = 11335;
$cli_num = 10;
$key_siz = 10000000;
$operation_dumpfile = "tmp-integration-recovery";
sub print_usage {
      print "Usage) perl ./integration/run_integration_recovery.pl\n";
}


use Cwd 'abs_path';
use File::Basename;


#######################################
# start node
#######################################
$cmd = "./integration/run.memcached.default.bash $t_port;";
system($cmd);

print "3 seconds sleep\n";
sleep 3;

#######################################
# node killed after $ktime seconds.
#######################################
$ktime = 20;
$cmd = "./integration/kill.memcached.perl $t_port $ktime &";
system($cmd);


#######################################
# start set operation
#######################################
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    #"zookeeper=127.0.0.1:9181\n" .
    #"service_code=test\n" .
    "single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=$cli_num\n" .
    "rate=0\n" .
    "request=0\n" .
    "time=600\n" .
    "keyset_size=$key_siz\n" .
    "valueset_min_size=20\n" .
    "valueset_max_size=20\n" .
    "pool=1\n" .
    "pool_size=20\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "operation_cli_dumpfile=" . $operation_dumpfile . "set-dumpfile" . "\n" .
    "client_exptime=120\n" .
    "client_timeout=5000\n" .
    "client_profile=integration_recovery_onlyset\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };

$ret = system($cmd);

if ($ret ne 0) {
    printf "TEST FAILED CODE=%d\n", $ret;
    printf "script name=integration_recovery_onlyset\n";
    exit(1);
}

print "sleep 3 seconds before engine restart";
sleep 3;

############################################
# engine restart
############################################
$cmd = "./integration/run.memcached.default.bash $t_port;";
system($cmd);

print "engine restart waitting recovery. 40 seconds sleep\n";
sleep 40;

############################################
# start get operation
###########################################
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    #"zookeeper=127.0.0.1:9181\n" .
    #"service_code=test\n" .
    "single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=$cli_num\n" .
    "rate=0\n" .
    "request=0\n" .
    "time=600\n" .
    "keyset_size=$key_siz\n" .
    "valueset_min_size=20\n" .
    "valueset_max_size=20\n" .
    "pool=1\n" .
    "pool_size=20\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "operation_cli_dumpfile=" . $operation_dumpfile . "get-dumpfile" . "\n" .
    "client_exptime=120\n" .
    "client_timeout=5000\n" .
    "client_profile=integration_recovery_onlyget\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };

$ret = system($cmd);

if ($ret ne 0) {
    printf "TEST FAILED CODE=%d\n", $ret;
    printf "script name=integration_recovery_onlyget\n";
    exit(1);
}

print "3 seconds sleep\n";
sleep 3;

###############################################
# compare file
###############################################
my $fnum = 0;
my $setfile;
my $getfile;
my $resultfile = "tmp-integration-recovery-result.txt";
system("echo -e \'\033[33mstart compare file....\033[0m\'");
while($fnum != $cli_num) {
    $setfile = $operation_dumpfile . "set-dumpfile" . $fnum . ".txt";
    $getfile = $operation_dumpfile . "get-dumpfile" . $fnum . ".txt";
    $cmd = "./integration/compare_for_recovery.pl $setfile $getfile $resultfile";
    $fnum++;
    $ret = system($cmd);
    if ($ret ne 0) {
        die;
    }
}
system("echo -e \'\033[33mend recovery test see $resultfile....\033[0m\'");
