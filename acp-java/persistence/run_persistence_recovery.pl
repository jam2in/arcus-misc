#!/usr/bin/perl -w

#####################################################
# use
#####################################################
use Term::ANSIColor qw(:constants);

#####################################################
# variable definition
#####################################################
$t_ip = "127.0.0.1";
$t_port = "11446";
$client = 1;
$rate = 0;
$request = 0;
$time = 0;
$keyset_size = 10000000;
$val_min = 8;
$val_max = 32;
$pool = 1;
$pool_size = 5;
$prefix = "persistence:";
$operation_dumpfile = "tmp-persistence-recovery";
$client_exptime = 0;
$client_timeout = 5000;
$ktime = 30;
$wtime = 40;

#####################################################
# remove ARCUS-DB files
#####################################################
my $errors;
while ($_ = glob('persistence/ARCUS-DB/*')) {
    next if -d $_;
    unlink($_)
      or ++$errors, warn("Can't remove $_: $!");
}
exit(1) if $errors;
sleep 2;

#####################################################
# remove operation dump files
#####################################################
while ($_ = glob("$operation_dumpfile*")) {
    next if -d $_;
    unlink($_)
      or ++$errors, warn("Can't remove $_: $!");
}
exit(1) if $errors;
sleep 2;

#####################################################
# start default engine node with persistence
#####################################################
print BRIGHT_YELLOW ">>> ENGINE START\n", RESET;
$cmd = "./persistence/run.memcached.persistence.bash $t_port default_engine_sync_logging.conf";
system($cmd);
sleep 2;

#####################################################
# kill default engine node after $ktime seconds
#####################################################
$cmd = "./persistence/kill.memcached.persistence.perl $t_port $ktime &";
system($cmd);
sleep 2;

#####################################################
# start set operation
#####################################################
print BRIGHT_YELLOW ">>> START persistence_recovery_onlyset\n", RESET;
open CONF, ">tmp-persistence-config.txt" or die $!;
print CONF
    "single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=$client\n" .
    "rate=$rate\n" .
    "request=$request\n" .
    "time=$time\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=$val_min\n" .
    "valueset_max_size=$val_max\n" .
    "pool=$pool\n" .
    "pool_size=$pool_size\n" .
    "pool_use_random=false\n" .
    "key_prefix=$prefix\n" .
    "operation_cli_dumpfile=" . $operation_dumpfile . "set-dumpfile" . "\n" .
    "client_exptime=$client_exptime\n" .
    "client_timeout=$client_timeout\n" .
    "client_profile=persistence_recovery_onlyset\n";
close CONF;

$cmd = "./run.bash -config tmp-persistence-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

$ret = system($cmd);
if ($ret ne 0) {
    printf BRIGHT_RED ">>> SCRIPT persistence_recovery_onlyset ERROR \n", RESET;
    exit(1);
}
sleep 2;

#####################################################
# engine restart
#####################################################
print BRIGHT_YELLOW ">>> ENGINE RESTART THEN WAITING RECOVERY $wtime SECONDS\n", RESET;
$cmd = "./persistence/run.memcached.persistence.bash $t_port default_engine_sync_logging.conf";
system($cmd);
sleep $wtime;

############################################
# start get operation
###########################################
print BRIGHT_YELLOW ">>> START persistence_recovery_onlyget\n", RESET;
open CONF, ">tmp-persistence-config.txt" or die $!;
print CONF
    "single_server=" . $t_ip . ":" . $t_port . "\n" .
    "client=$client\n" .
    "rate=$rate\n" .
    "request=$request\n" .
    "time=$time\n" .
    "keyset_size=$keyset_size\n" .
    "valueset_min_size=$val_min\n" .
    "valueset_max_size=$val_max\n" .
    "pool=$pool\n" .
    "pool_size=$pool_size\n" .
    "pool_use_random=false\n" .
    "key_prefix=$prefix\n" .
    "operation_cli_dumpfile=" . $operation_dumpfile . "get-dumpfile" . "\n" .
    "client_exptime=$client_exptime\n" .
    "client_timeout=$client_timeout\n" .
    "client_profile=persistence_recovery_onlyget\n";
close CONF;

$cmd = "./run.bash -config tmp-persistence-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

$ret = system($cmd);
if ($ret ne 0) {
    printf BRIGHT_RED ">>> SCRIPT persistence_recovery_onlyget ERROR \n", RESET;
    exit(1);
}
sleep 2;

#####################################################
# kill default engie nnode
#####################################################
$cmd = "./persistence/kill.memcached.persistence.perl $t_port &";
system($cmd);

#####################################################
# compare file
#####################################################
my $fnum = 0;
my $setfile;
my $getfile;
my $resultfile = "tmp-persistence-recovery-result.txt";
print BRIGHT_YELLOW ">>> START SET/GET COMPARATION\n", RESET;
while($fnum != $client) {
    $setfile = $operation_dumpfile . "set-dumpfile" . $fnum . ".txt";
    $getfile = $operation_dumpfile . "get-dumpfile" . $fnum . ".txt";
    $cmd = "./persistence/run_persistence_compare_for_recovery.pl $setfile $getfile $resultfile";
    $fnum++;
    $ret = system($cmd);
    if ($ret ne 0) {
        print BRIGHT_RED "##############################\n", RESET;
        print BRIGHT_RED "# TEST FAILED\n", RESET;
        print BRIGHT_RED "# CHECKOUT $setfile, $getfile, $resultfile FILE\n", RESET;
        print BRIGHT_RED "##############################\n", RESET;
        exit(1);
    }
}
print BRIGHT_GREEN "##############################\n", RESET;
print BRIGHT_GREEN "# TEST SUCCESS \n", RESET;
print BRIGHT_GREEN "##############################\n", RESET;
