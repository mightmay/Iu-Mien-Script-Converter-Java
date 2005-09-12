#!/usr/bin/perl
use Encode::UTR22;
use Getopt::Long;

GetOptions('byte|b=s' => \$opt_b,
           'unicode|u=s' => \$opt_u,
           'both|bu=s' => \$opt_bu,
           'outfile=s' => \$opt_o);

unless (defined $ARGV[0])
{
    die <<'EOT';
    testutr22 [-b byte.xml] [-u uni.xml] [-bu enc.xml] [-o outfile] "test string"
Runs the test string through the encoding converters listed in debug mode. If
there is a -b option, then the input is considered to be in bytes. The string
is a list of 2 character hex byte codes: 48 65 6C 6C 6F. If there is no -b
option, then the input is considered to be Unicode represented as 4-6 character
hex codes.

    -b enc_file     Encoding file to use for byte to Unicode conversion
    -bu enc_file    Encoding file to use for both directions of conversion
    -o out_file     Output file for report
    -u enc_file     Encoding file to use for Unicode to byte conversion
EOT
}

$opt_b = $opt_bu if ($opt_bu && !$opt_b);
$opt_u = $opt_bu if ($opt_bu && !$opt_u);

open (STDOUT, "> $opt_o") if $opt_o;

if ($opt_b)
{
    $enc_in = Encode::UTR22->new($opt_b, 'debug' => 1) || die "Can't find $opt_b";
    $str = pack('C*', map {hex($_)} split (' ', $ARGV[0]));
    
    ($out, $debug) = $enc_in->debug_decode($str);
    print $debug;
    print "\n-------------" . join(" ", map {sprintf("%04X", $_)} 
                                 unpack('U*', $out)) . "-------------\n";
}

if ($opt_u)
{
    unless ($out)
    { $out = pack('U*', map {hex($_)} split(' ', $ARGV[0])); }

    $enc_out = Encode::UTR22->new($opt_u, 'debug' => 1) || die  "Can't find $opt_u";
    ($final, $debug) = $enc_out->debug_encode($out);
    print $debug;
    print "\n-------------" . join(" ", map {sprintf("%02X", $_)} 
                                 unpack('C*', $final)) . "-------------\n";
}
