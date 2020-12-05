#!/usr/bin/perl -w
$t_ip = "127.0.0.1";   # test ip
$t_port = "11446"; # test port
print "test_ip = $t_ip, test_port = $t_port\n";

$request = 1;
$key_siz = 1;
$val_min = 100;
$val_max = 1000;
$prefix = "";

use Cwd 'abs_path';
use File::Basename;

#NOTE : need to amend port in CommandFlush.java when test flush_all
@script_list = (
  "Flush"
, "Simple"
, "List"
, "Set"
, "Map"
, "Btree"
, "Attr"
);

#remove files.(ARCUS-DB/*).
my $errors;
while ($_ = glob('persistence/ARCUS-DB/*')) {
    next if -d $_;
    unlink($_)
      or ++$errors, warn("Can't remove $_: $!");
}
exit(1) if $errors;
sleep 1;

############################################
# start default engine node with persistence.
############################################
$cmd = "./persistence/run.memcached.persistence.bash $t_port";
system($cmd);

print "2 seconds sleep\n";
sleep 2;

foreach $script (@script_list) {
############################################
# start set operation
############################################
open CONF, ">tmp-persistence-config.txt" or die $!;
print CONF
    #"zookeeper=127.0.0.1:9181\n" .
    #"service_code=test\n" .
    "single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=1\n" .
    "rate=0\n" .
    "request=$request\n" .
    "keyset_size=$key_siz\n" .
    "valueset_min_size=$val_min\n" .
    "valueset_max_size=$val_max\n" .
    "pool=1\n" .
    "pool_size=1\n" .
    "pool_use_random=false\n" .
    "key_prefix=$prefix\n" .
    "client_exptime=0\n" .
    "client_timeout=5000\n" .
    "client_profile=" . $script . "RequestTest" . "\n";
close CONF;

$cmd = "./run.bash -config tmp-persistence-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

$ret = system($cmd);
if ($ret ne 0) {
    printf "TEST FAILED CODE=%d\n", $ret;
    printf "script name=%sRequestTest\n", $script;
    exit(1);
}
}

print "sleep 3 seconds before engine restart\n";
sleep 3;

#######################################
# node killed.
#######################################
$cmd = "./persistence/kill.memcached.persistence.perl $t_port &";
system($cmd);
sleep 3;


############################################
# engine restart
############################################
$cmd = "./persistence/run.memcached.persistence.bash $t_port";
system($cmd);

$sleep = 15;
print "engine restart waitting recovery. $sleep seconds sleep\n";
sleep 15;


foreach $script (@script_list) {
############################################
# start get operation
############################################
open CONF, ">tmp-persistence-config.txt" or die $!;
print CONF
    #"zookeeper=127.0.0.1:9181\n" .
    #"service_code=test\n" .
    "single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=1\n" .
    "rate=0\n" .
    "request=$request\n" .
    "keyset_size=$key_siz\n" .
    "valueset_min_size=$val_min\n" .
    "valueset_max_size=$val_max\n" .
    "pool=1\n" .
    "pool_size=1\n" .
    "pool_use_random=false\n" .
    "key_prefix=$prefix\n" .
    "client_exptime=0\n" .
    "client_timeout=5000\n" .
    "client_profile=" . $script . "ConfirmTest" . "\n";
close CONF;

$cmd = "./run.bash -config tmp-persistence-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

$ret = system($cmd);

if ($ret ne 0) {
    printf "TEST FAILED CODE=%d\n", $ret;
    printf "script name=%sConfirmTest\n", $script;
    exit(1);
}
}

$cmd = "./run.bash -config config-tested-print.txt";
$ret = system($cmd);
if ($ret ne 0) {
    printf "TEST FAILED CODE=%d\n", $ret;
    printf "script name=%s\n", $script;
    exit(1);
}


