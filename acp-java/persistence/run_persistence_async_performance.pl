#!/usr/bin/perl -w

######################################################
# use
######################################################
use Term::ANSIColor qw(:constants);
use Cwd qw(getcwd);

######################################################
# client conf variable
######################################################
$t_ip = "jam2in-m001";
$t_port = "11446";
$client = 256;
$rate = 0;
$time_only_set = 0;
$time_get_set_ratio = 360;
$request_only_set = 40000;
$request_get_set_ratio = 0;
$keyset_size = 10000000;
$val_min = 8;
$val_max = 32;
$pool = 1;
$pool_size = 256;
$prefix = "persistence:";

######################################################
# estimate variable
######################################################
$est_rps = 50000;
$est_res = 8000000;

######################################################
# remote remove ARCUS-DB files
######################################################
$dir = getcwd;
$cmd = "ssh $t_ip 'cd $dir && rm -f persistence/ARCUS-DB/*'";
system($cmd);
sleep 2;

######################################################
# remote start default engine node with persistence.
######################################################
print BRIGHT_YELLOW ">>> ENGINE START\n", RESET;
$cmd = "ssh $t_ip 'cd $dir && ./persistence/run.memcached.persistence.bash $t_port default_engine_async_logging.conf' &";
system($cmd);
sleep 2;

######################################################
# run client
######################################################
@script_list = (
    "persistence_onlyset",
    "persistence_getset_ratio"
);

foreach $script (@script_list) {
    print BRIGHT_YELLOW ">>> START $script\n", RESET;

    if ($script eq "persistence_onlyset") {
      $time = $time_only_set;
      $request = $request_only_set;
    } else {
      $time = $time_get_set_ratio;
      $request = $request_get_set_ratio;
    }

    $result_filename = "tmp-persistence-async-performance-summary.txt";

    # Create a temporary config file to run the test
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
        "generate_resultfile=$result_filename\n" .
        "client_exptime=0\n" .
        "client_timeout=5000\n" .
        "client_profile=" . $script . "\n";
    close CONF;

    $cmd = "./run.bash -config tmp-persistence-config.txt";
    printf "RUN COMMAND=%s\n", $cmd;

    $ret = system($cmd);
    if ($ret ne 0) {
        printf BRIGHT_RED ">>> SCRIPT $script ERROR \n", RESET;
        last;
    } elsif ($script eq "persistence_getset_ratio") {
        print BRIGHT_YELLOW ">>> PERFORMANCE CHECK\n", RESET;
        $rps_success = 0;
        $res_success = 0;
        open (TEXT, $result_filename);
        while(<TEXT>) {
            $line = $_;
            @result = split/=/,$line;
            if ($result[0] eq "requests/s"){
                $real_rps = $result[1];
                if ($result[1] >= $est_rps) {
                    $rps_success = 1;
                }
            } elsif ($result[0] eq "response_time(ns)"){
                $real_res = $result[1];
                if ($result[1] <= $est_res) {
                    $res_success = 1;
                }
            }
        }
        close(TEXT);
    }
}

######################################################
# remote kill default engine node
######################################################
$cmd = "ssh $t_ip 'cd $dir && ./persistence/kill.memcached.persistence.perl $t_port &'";
system($cmd);
sleep 2;

######################################################
# print test result
######################################################
if ($ret eq 0) {
    print BRIGHT_YELLOW "RESULT RPS >>> estimated: $est_rps, real: $real_rps\n", RESET;
    print BRIGHT_YELLOW "RESULT RES(ns) >>> estimated: $est_res, real: $real_res\n", RESET;
    if ($rps_success eq 1 && $res_success eq 1) {
        print BRIGHT_GREEN "##############################\n", RESET;
        print BRIGHT_GREEN "# TEST SUCCESS \n", RESET;
        print BRIGHT_GREEN "##############################\n", RESET;
    } else {
        print BRIGHT_RED "##############################\n", RESET;
        print BRIGHT_RED "# TEST FAILED\n", RESET;
        print BRIGHT_RED "##############################\n", RESET;
    }
} else {
    print BRIGHT_RED "##############################\n", RESET;
    print BRIGHT_RED "# TEST ERROR\n", RESET;
    print BRIGHT_RED "##############################\n", RESET;
    exit(1);
}
