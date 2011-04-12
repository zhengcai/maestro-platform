#!/usr/bin/perl -w
# The script to run Maestro in background, binding threads to cores

my $javapath = $ENV{'JAVA_HOME'} or die "Please configure JAVA_HOME to the right jdk directory";
my $configfile = $ARGV[0] or die "Run with: parameter-configuration-file dag-file";
my $dagfile = $ARGV[1] or die "Run with: parameter-configuration-file dag-file";

system("$javapath/bin/java -cp build/ sys/Main $configfile $dagfile daemon &");#2>err.log &");
sleep(1);

#=begin comment
my $threadsnum = 0;

open CONF, $configfile;
my @ls = <CONF>;

foreach my $l (@ls) {
    chomp($l);
    my @words = split(/ /, $l);
    if ($words[0] =~ /numThreads/) {
	$threadsnum = $words[1];
	last;
    }
}

my @psresult = split(/\n/, `ps -eLF | grep java | grep -v grep`);
my @tids;
foreach $psline (@psresult) {
    my @fields = split(/ +/, $psline);
    push(@tids, $fields[3]);
}

my $flag = 1;
for (my $i=0;$i<scalar @tids;$i++) {
    if ((scalar @tids - $i) <= $threadsnum) {
	my $mask = sprintf("%x", $flag);
	system("taskset -p $mask $tids[$i]");
	$flag = $flag * 2;
	if ($flag > 128) {
	    $flag = 1;
	}
    }
}	
