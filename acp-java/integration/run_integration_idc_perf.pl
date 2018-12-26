#!/usr/bin/perl -w
$t_ip = 0;   # test ip
$t_port = 0; # test port
$est_perf = 30000; # estimated performance
$client = 256;
$timeout = 1000;
$idc_zkip = "127.0.0.1";
sub print_usage {
      print "Usage) perl ./integration/run_integration_perf.pl [IDC_ZKIP]\n";
}

if ($#ARGV le 0) {
    if ($#ARGV eq 1) {
        $idc_zkip = "$ARGV[0]";
    }
    print "idc_zookeeper_ip = $idc_zkip\n";
} else {
    print_usage();
    die;
}

use Cwd 'abs_path';
use File::Basename;

@script_list = (
    "integration_onlyset"      # only set operation
  , "integration_getset_ratio" # get/set ratio 4:1 performance test
);

foreach $script (@script_list) {
    $result_filename = "tmp-integration-perf-summary.txt"; #generate result file
    # Create a temporary config file to run the test
    my $time;
    if ($script eq "integration_onlyset") {
      $time = -1;
    } else {
      $time = 240;
    }
    open CONF, ">tmp-integration-config.txt" or die $!;
    print CONF
        "zookeeper=$idc_zkip:9181\n" .
        "service_code=test_idc\n" .
        "client=$client\n" .
        "rate=0\n" .
        "request=0\n" .
        "time=$time\n" .
        "keyset_size=10000000\n" .
        "valueset_min_size=8\n" .
        "valueset_max_size=32\n" .
        "pool=2\n" .
        "pool_size=128\n" .
        "pool_use_random=false\n" .
        "key_prefix=integrationtest:\n" .
        "generate_resultfile=" . $result_filename . "\n" .
        "client_exptime=0\n" .
        "client_timeout=$timeout\n" .
        "client_profile=" . $script . "\n";
    close CONF;

    $cmd = "./run.bash -config tmp-integration-config.txt";
    printf "RUN COMMAND=%s\n", $cmd;

    local $SIG{TERM} = sub { print "TERM SIGNAL\n" };

    $ret = system($cmd);

    if ($ret eq 0) {
        print "performance check...\n";
        $success = 0;
        open (TEXT, $result_filename);
        while(<TEXT>) {
            $line = $_;
            @result = split/=/,$line;
            if ($result[0] eq "requests/s"){
                $real_perf = $result[1];
                if ($result[1] >= $est_perf) {
                    $success = 1;
                }
            }
        }
        close(TEXT);
    } else {
        last;
    }
}

###############################
# for check response time
###############################

$result_filename = "tmp-integration-perf-summary_responseTime.txt"; #generate result file
# Create a temporary config file to run the test
my $time;
open CONF, ">tmp-integration-config.txt" or die $!;
print CONF
    "zookeeper=$idc_zkip:9181\n" .
    "service_code=test_idc\n" .
    "client=1\n" .
    "rate=0\n" .
    "request=0\n" .
    "time=60\n" .
    "keyset_size=10000000\n" .
    "valueset_min_size=8\n" .
    "valueset_max_size=32\n" .
    "pool=2\n" .
    "pool_size=128\n" .
    "pool_use_random=false\n" .
    "key_prefix=integrationtest:\n" .
    "generate_resultfile=" . $result_filename . "\n" .
    "client_exptime=0\n" .
    "client_timeout=$timeout\n" .
    "client_profile=integration_getset_ratio\n";
close CONF;

$cmd = "./run.bash -config tmp-integration-config.txt";
printf "RUN COMMAND=%s\n", $cmd;

local $SIG{TERM} = sub { print "TERM SIGNAL\n" };

$ret = system($cmd);

if ($ret eq 0) {
    print "RESULT >>> estimated perf : $est_perf, real perf : $real_perf\n";
    if ($success eq 1) {
        print "############################\n";
        print "SUCCESS PERFORMANCE TEST\n";
        print "############################\n";
    } else {
        print "############################\n";
        print "FAILED PERFORMANCE TEST\n";
        print "############################\n";
    }
} else {
    print "############################\n";
    print "exit with ERROR CODE : $ret\n";
    print "############################\n";
}
