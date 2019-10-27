#!/usr/bin/perl -w

use strict;

my $k_port = 0; # memcached kill port

sub print_usage {
  print "Usage) perl ./integration/kill.memcached.perl <kill_port>\n";
}

if ($#ARGV == 0) {
  $k_port = $ARGV[0];
} else {
  print_usage();
  die;
}

my $cmd = "kill -9 \$(ps -ef | grep $k_port | awk '{print \$2}')";
#my $cmd = "kill -9 \$(ps -ef | awk '/sync.config; -p $k_port/ {print \$2}')";
#my $cmd = "kill \$(ps -ef | awk '/-b 8192 -m2048 -p $k_port/ {print \$2}')";
printf "RUN COMMAND = $cmd\n";
system($cmd);

