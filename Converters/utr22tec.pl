#!/usr/bin/perl
use Encode::UTR22;
use Getopt::Std;
use IO::File;
use Compress::Zlib;

$VERSION = 0.02;    # MJPH   7-JUL-2002     add bctxt to re-order rules
# $VERSION = 0.01;    # MJPH   7-SEP-2002     Original

getopts('z');

*error = \&Encode::UTR22::error;
*strerror = \&Encode::UTR22::strerror;

unless (defined $ARGV[1])
{
    die <<'EOT';
    utr22tec [-z] infile outfile
Compiles the given UTR22 XML mapping description into a SILtec binary file
for use with SILtec.

    -z  Produce uncompressed file
EOT
}

$enc = Encode::UTR22->process_file($ARGV[0]) || die "Can't read mapping file $ARGV[0]";

if (!$opt_z)
{ $outfh = IO::File->new_tmpfile() || die "Can't create temporary file"; }
else
{ $outfh = IO::File->new("> $ARGV[1]") || die "Can't open $ARGV[1] for writing"; }
binmode $outfh;

#foreach $r (sort {$self->{'contexts'}{$a}{'line'} <=> $self->{'contexts'}{$b}{'line'}}
#            keys %{$self->{'contexts'}})
#{ $self->{'regexps'}{$r} = $self->{'contexts'}{$r}->asTec(); }

$hdr = create_header($outfh, $enc);
push (@tables, pad($outfh));
if ($enc->{'orders'}{'bytes'})
{
    compile_order($enc, 0, 'bytes', $outfh);
    push (@tables, pad($outfh));
}
compile_map($enc, 0, $outfh);
push (@tables, pad($outfh));
if ($enc->{'orders'}{'unicode'})
{
    compile_order($enc, 0, 'unicode', $outfh);
    push (@tables, pad($outfh));
    compile_order($enc, 1, 'unicode', $outfh);
    push (@tables, pad($outfh));
}
compile_map($enc, 1, $outfh);
push (@tables, pad($outfh));
if ($enc->{'orders'}{'bytes'})
{
    compile_order($enc, 1, 'bytes', $outfh);
    push (@tables, pad($outfh));
}

delete $tables[-1];
$outfh->seek($hdr, 0);
$outfh->print(pack('N*', @tables));
$outfh->seek(0, 2);

if (!$opt_z)
{
    my ($str);
    my ($d, $status) = deflateInit();
    $finfh = IO::File->new("> $ARGV[1]") || die "Can't open $ARGV[1] for writing";
    binmode $finfh;
    $len = $outfh->tell();
    $finfh->print(pack('a4N', 'zQmp', $len)) if ($d);
    $outfh->seek(0, 0);
    
    while ($outfh->read($dat, 4096))
    {
        if ($d)
        {
            ($str, $status) = $d->deflate($dat);
            $finfh->print($str);
        }
        else
        { $finfh->print($dat); }
    }
    if ($d)
    {
        ($str, $status) = $d->flush();
        $finfh->print($str);
    }
    $finfh->close();
}

$outfh->close();

sub max
{ $_[0] > $_[1] ? $_[0] : $_[1]; }

sub pad
{
    my ($fh) = @_;
    my ($loc) = $fh->tell();
    
    $fh->print("\000" x (4 - ($loc & 3))) if ($loc & 3);
    return $fh->tell();
}

sub create_header
{
    my ($outfh, $enc) = @_;
    my ($val, $count, $str, @strs, $numtables, $normal, $stroffset, $noffset);
    
    $count = -1;
    foreach $val ($enc->{'info'}{'id'}, 'Unicode', $enc->{'info'}{'description'}, 'Unicode',
                  $enc->{'info'}{'version'}, $enc->{'info'}{'contact'}, $enc->{'info'}{'registrationAuthority'},
                  $enc->{'info'}{'registrationName'}, $enc->{'info'}{'copyright'})
    {
        $count++;
        next unless $val;
        $str = pack('n n/a*', $count, $val);
        $str .= "\000" if (length($str) & 1);
        push (@strs, $str);
    }
    
    $numtables = 1;
    $numtables++ if (scalar @{$enc->{'orders'}{'unicode'}});
    $numtables++ if (scalar @{$enc->{'orders'}{'bytes'}});

    $normal = 0;
    if ($enc->{'info'}{'normalization'} eq 'NFD')
    { $normal = 10; }
    elsif ($enc->{'info'}{'normalization'} eq 'NFC')
    { $normal = 5; }
    elsif ($enc->{'info'}{'normalization'} eq 'NFC_NFD')
    { $normal = 12; }
    $normal |= 0x10000;
    
    $noffset = (8 + $numtables * 2 + (scalar @strs)) * 4;
    foreach $val (@strs)
    {
        $stroffset .= pack('N', $noffset);
        $noffset += length($val);
    }
    
    $outfh->print(pack("a4N7a*N${numtables}N${numtables}a*",
        'qMap', 0x00020001, $noffset, 0, $normal, scalar @strs, $numtables, $numtables,
        $stroffset,
        (0) x $numtables, (0) x $numtables, join('', @strs)));
        
    return (8 + (scalar @strs)) * 4;
}

sub compile_map
{
    my ($self, $toBytes, $outfh) = @_;
    my ($srcl) = $toBytes ? 'u' : 'b';
    my ($destl) = $toBytes ? 'b' : 'u';
    my ($r, $res, @res, $c, $pre, $post, $lpre, $lpost, $line, $first, $dump); 
    my (@classes, $surrogates, $match, $lmatch, $gen, $lgen, $k, $ocount, $rcount, @rules);
    my ($mpre, $mmatch, $mpost, $mgen);

    return $self if ($self->{"${srcl}conv"});

    foreach $r (@{$self->{'rules'}})
    {
        next if ($r->{'type'} ne 'a' && (($toBytes == 1) ^ ($r->{'type'} eq 'fub')));
        $pre = ''; $post = ''; $res = ''; $lpre = 0; $lpost = 0;
        $line = $r->{'line'};
        
        if ($r->{"${srcl}bctxt"})
        {
            error (undef, undef, "No regexp " . $r->{"${srcl}bctxt"} . " for ${srcl}bctxt at line $r->{line}")
                    unless ($self->{'contexts'}{$r->{"${srcl}bctxt"}});
            ($pre, $dump, $lpre) = @{$self->{'contexts'}{$r->{"${srcl}bctxt"}}->
                                        asTec(pass => "${srcl}map", reverse => 1)};
        }

        $res = $r->{$srcl};
        error (undef, undef, "Empty mapping to " . strerror($r->{$destl}, $toBytes) . " not allowed")
                if ($res eq '');

        if ($r->{"${srcl}actxt"})
        {
            error (undef, undef, "No regexp " . $r->{"${srcl}actxt"} . " for ${srcl}actxt at line $r->{line}")
                    unless ($self->{'contexts'}{$r->{"${srcl}actxt"}});
            ($post, $dump, $lpost) = @{$self->{'contexts'}{$r->{"${srcl}actxt"}}->asTec(pass => "${srcl}map")};
        }

        @res = unpack($toBytes ? 'U*' : 'C*', $res);
        $first = pack($toBytes ? 'U' : 'C', $res[0]);
        $lmatch = 0; $match = '';
        foreach $c (@res)
        { 
            $match .= pack('CCn', 0x11, $c >> 16, $c & 0xFFFF);
            $surrogates = 1 if ($c > 0xFFFF);
            $lmatch++;
        }
        
        @res = unpack($toBytes ? 'C*' : 'U*', $r->{$destl});
        $lgen = 0; $gen = '';
        foreach $c (@res)
        {
            $gen .= pack('CCn', 0, $c >> 16, $c & 0xFFFF);
            $lgen++;
        }
        
        push (@{$self->{"${srcl}conv"}{$first}}, [$pre, $lpre, $match, $lmatch, $post, $lpost, $gen, $lgen, $r]);
        $mpre = max($mpre, $lpre);
        $mmatch = max($mmatch, $lmatch);
        $mpost = max($mpost, $lpost);
        $mgen = max($mgen, $lgen);
    }
    
    foreach $k (keys %{$self->{'classes'}})
    {
        $r = $self->{'classes'}{$k};
        if (defined $r->{'tecRef'}{"${srcl}map"})
        {
            $classes[$r->{'tecRef'}{"${srcl}map"}] = $r;
            if ($toBytes && !$surrogates)
            {
                foreach $c (@{$r->{'elements'}})
                { $surrogates = 1 if (unpack('U', $c) > 0xFFFF); }
            }
        }
    }
    
    $rcount = 0;
    foreach $k (sort keys %{$self->{"${srcl}conv"}})
    {
        $r = $self->{"${srcl}conv"}{$k};
        @{$r} = sort {$b->[8]{'priority'} <=> $a->[8]{'priority'}
                    || $b->[3] <=> $a->[3] 
                    || ($b->[1] + $b->[5]) <=> ($a->[1] + $a->[5])
                    || $a->[8]{'line'} <=> $b->[8]{'line'}} @{$r};
        
        error(undef, undef, "Ambiguous mapping from " . strerror($k, !$toBytes) . 
                " at lines $r[-2][8]{'line'} and $r[-1][8]{'line'}")
                if (scalar @{$r} > 1 && $r->[-2][3] == 1 && $r->[-2][1] == 0 && $r->[-2][5] == 0);
        if (scalar @{$r} == 1)
        {
            if ($r->[0][1] != 0 || $r->[0][5] != 0)
            { error(undef, undef, "No default mapping for ". strerror($k, !$toBytes)); }
            elsif ($toBytes && $r->[0][7] < 4)
            {
                $self->{"${srcl}map"}{$k} = pack('C4', length($r->[0][8]{$destl}), unpack('C*', $r->[0][8]{$destl}));
                next;
            }
            elsif (!$toBytes && $r->[0][7] < 2)
            {
                my ($gen) = unpack('U', $r->[0][8]{$destl});
                $self->{"${srcl}map"}{$k} = pack('CCn', 0, $gen >> 16, $gen & 0xFFFF);      #opcode should be 1
                next;
            }
        }
        $ocount = $rcount;
        foreach $c (@{$r})
        {
            push (@rules, pack('CCCCa*a*a*a*', length($c->[2]) / 4, length($c->[4]) / 4, length($c->[0]) / 4,
                    length($c->[6]) / 4, $c->[2], $c->[4], $c->[0], $c->[6]));
            $rcount++;
        }
        $self->{"${srcl}map"}{$k} = pack('CCn', 0xFF, $rcount - $ocount, $ocount);
    }
    
    $replace = unpack($toBytes ? 'C' : 'U', $self->{'sub'}[!$toBytes]);
    
    output_map($self->{"${srcl}map"}, \@rules, \@classes, $mpre, $mmatch, $mpost, $mgen, $replace, $toBytes, $surrogates, 0, $outfh);
}

sub output_map
{
    my ($map, $rules, $classes, $mpre, $mmatch, $mpost, $mgen, $rep, $toBytes, $surr, $overlap, $fh, $roffsets) = @_;
    my ($loc) = $fh->tell();
    my ($src) = ('B', 'U')[$toBytes];
    my ($dest) = ('U', 'B')[$toBytes];
    my ($p, @unimap, @pmap, @cmap, @lkups, @roffs, @tclass);
    my ($poffset, $loffset, $roffset, $rooffset, $coffset, $foffset, $k, $c, $offs);
    
    $dest = $src if $overlap;
    $fh->print(pack('a4NN8C4N', "${src}\->${dest}", 0x00020000, 0, (!$toBytes && $surr), (0) x 6, $mmatch, $mpre, $mpost, $mgen, $rep));

    if ($toBytes)
    {
        $chid = 1; $sid = 0; $pid = 0;
        $lkups[0] = pack('CCn', 0xFD, 0, 0);
        foreach $k (sort keys %{$map})
        {
            $c = unpack('U', $k);
            if (!defined $unimap[$c >> 16])
            { $unimap[$c >> 16] = $sid++; }
            if (!defined $pmap[$unimap[$c >> 16]][($c & 0xFF00) >> 8])
            { $pmap[$unimap[$c >> 16]][($c & 0xFF00) >> 8] = $pid++; }
            $cmap[$pmap[$unimap[$c >> 16]][($c & 0xFF00) >> 8]][$c & 0xFF] = $chid;
            $lkups[$chid] = $map->{$k};
            $chid++;
        }
        
        $poffset = $fh->tell() - $loc;
        if ($surr)
        {
            $fh->print(pack('C17', map {defined $unimap[$_] ? $unimap[$_] : 0xFF} (0 .. 16)));
            $fh->print(pack('Cn', $sid, 0));
        }
        foreach $p (@pmap)
        { $fh->print(pack('C256', map {defined $p->[$_] ? $p->[$_] : 0xFF} (0 .. 255))); }
        foreach $p (@cmap)
        { $fh->print(pack('n256', @{$p})); }
        $loffset = $fh->tell() - $loc;
        foreach $p (@lkups)
        { $fh->print($p); }
    }
    else
    {
        $loffset = $fh->tell() - $loc;
        foreach $p (0 .. 255)
        {
            if (defined $map->{pack('C', $p)})
            { $fh->print($map->{pack('C', $p)}); }
            else
            { $fh->print(pack('CCn', 0xFD, 0, 0)); }
        }
    }

    $roffset = $fh->tell() - $loc;
    foreach $p (@{$rules})
    {
        push (@roffs, $offs) unless ($overlap);
        $fh->print($p);
        $offs += length($p);
    }
    
    $rooffset = $fh->tell() - $loc;
    if ($overlap)
    {
        foreach $p (@{$roffsets})
        { $fh->print(pack('N', $p)); }
    }
    else
    {
        foreach $p (@roffs)
        { $fh->print(pack('N', $p)); }
    }
    
    $coffset = $fh->tell() - $loc;
    my (@tclass, $t, $toff);
    $toff = 4 * (scalar @{$classes});
    foreach $p (@{$classes})
    { 
        $t = $p->asTec($toBytes * ($surr + 1));
        push (@tclass, $t);
        $fh->print(pack('N', $toff));
        $toff += length($t);
    }
    foreach $p (@tclass)
    { $fh->print($p); }
    
    $foffset = $fh->tell() - $loc;
    $fh->seek($loc + 8, 0);
    $roffset = $foffset if ($roffset == $rooffset);
    $rooffset = $foffset if ($rooffset == $coffset);
    $fh->print(pack('N8', $foffset, (!$toBytes && $surr), $poffset, $loffset, $coffset, $foffset, 
                          $rooffset, $roffset));
    $fh->seek($foffset + $loc, 0);
}

sub compile_order
{
    my ($self, $toBytes, $side, $outfh) = @_;
    my ($srcl) = $toBytes ? 'u' : 'b';
    my ($destl) = $toBytes ? 'b' : 'u';
    my ($output) = $toBytes ? 'uorder' : 'border';
    my ($isBytes) = ($side eq 'bytes');
    my ($count, $obj, $r, $tec, $list, $dummy, $i, $name, $reg1, $num, $outtec);
    my ($lkup, @teclist, @toffsets, @moffsets, $k, $c, $surrogates, $lmatch, $lgen, $mmatch, $mgen);
    my ($mpre, $mpost);
    my (@classes, $ocount, $vcount, %firsts, %map, %vals);

    foreach $r (@{$self->{'orders'}{$side}})
    {
        my (%names, @nums, $outtec);
        my ($btec, $lpre, $atec, $lpost);
        my ($reg1, $list, $dummy);
        my ($tec, $list, $lmatch) = @{$self->{'contexts'}{$r->{$srcl}}->asTec(collect => 1, pass => "$srcl$side")};
        if ($r->{'bctxt'})
        { ($btec, $dummy, $lpre) = @{$self->{'contexts'}{$r->{'bctxt'}}->asTec(reverse => 1, pass => "$srcl$side")}; }
        else
        { $btec = ''; $lpre = 0; }
        
        if ($r->{'actxt'})
        { ($atec, $dummy, $lpost) = @{$self->{'contexts'}{$r->{'bctxt'}}->asTec(pass => "$srcl$side")}; }
        else
        { $atec = ''; $lpost = 0; }
        
        error(undef, undef, "Match regexp too long at line $r->{'line'}") if (length($tec) > 1024);
        foreach $i (keys %{$list})
        {
            $name = $i;
            if ($name =~ s{^\Q$r->{$srcl}\E(?:/|$)}{})
            { $name =~ s{^\Q$r->{$destl}\E(?:/|$)}{}; }
#                next unless ($name !~ m|/|o && $name ne '');
            next if ($name =~ m|/$|o);
            $names{$name} = $list->{$i};
        }

        ($reg1, $list, $dummy) = @{$self->{'contexts'}{$r->{$destl}}->asTec(collect => 1, pass => "$srcl$side")};
        foreach $i (sort {$list->{$a} <=> $list->{$b}} keys %{$list})
        {
            $name = $i;
            if ($name =~ s{^\Q$r->{$destl}\E(?:/|$)}{})
            { $name =~ s{^\Q$r->{$srcl}\E(?:/|$)}{}; }
#                next unless ($name !~ m|/|o && $name ne '');
            next if ($name =~ m|/$|o);
            push (@nums, $names{$name}) if ($name && defined $names{$name});
        }
        
        $lgen = 0;
        foreach $num (@nums)
        { 
            $outtec .= pack('CCn', 7, $num, 0); 
            $lgen++;
        }
        
        $mmatch = $lmatch if ($lmatch > $mmatch);
        $mgen = $lgen if ($lgen > $mgen);
        $mpre = $lpre if ($lpre > $mpre);
        $mpost = $lpost if ($lpost > $mpost);
        
        push (@toffsets, length($lkup));
        $lkup = pack('CCCCa*a*a*a*', length($tec) / 4, length($atec) / 4, length($btec) / 4, length($outtec) / 4, $tec, $atec, $btec, $outtec);
        push (@teclist, $lkup);
        
        foreach $num ($self->{'contexts'}{$r->{$srcl}}->findfirst())
        { $firsts{$num} |= (1 << $count); }
        
        $count++;
        error(undef, undef, "Can't handle more than 32 ordering rules per order at line $r->{'line'}")
            if ($count > 31);
    }
    
    $vcount = 0;
    foreach $k (sort keys %firsts)
    {
        unless (defined ($vals{$firsts{$k}}))
        { 
            $ocount = $vcount;
            for ($i = 0, $j = 1; $j <= $firsts{$k}; $i++, $j <<= 1)
            {
                push(@moffsets, $toffsets[$i]);
                $vcount++;
            }
            $vals{$firsts{$k}} = pack('CCn', 0xFF, ($vcount - $ocount), $ocount);
        }
        $map{$k} = $vals{$firsts{$k}}; 
    }
    
    foreach $k (keys %{$self->{'classes'}})
    {
        $r = $self->{'classes'}{$k};
        if (defined $r->{'tecRef'}{"$srcl$side"})
        {
            $classes[$r->{'tecRef'}{"$srcl$side"}] = $r;
            if (!$isBytes && !$surrogates)
            {
                foreach $c (@{$r->{'elements'}})
                { $surrogates = 1 if (unpack('U', $c) > 0xFFFF); }
            }
        }
    }
    
    output_map(\%map, \@teclist, \@classes, $mpre, $mmatch, 0, $mgen, 0, $side eq 'unicode', $surrogates, 1, $outfh, \@moffsets);
}

package Encode::UTR22::Regexp::Element;

sub base
{ $_[0]; }

package Encode::UTR22::Regexp::Group;

sub asTec
{
    my ($self, %opts) = @_;
    my ($r, $res, $names, $count, $text, $sub, $subl, $lacc, $asgroup, $oroff);
    my ($min, $max, $index, $type);

    $min = defined $self->{'min'} ? $self->{'min'} : 1;
    $max = defined $self->{'max'} ? $self->{'max'} : 1;

    $names = {};
    
    if ($max != 1 || $min != 1 || ($opts{'collect'} && $self->{'id'}) || defined $self->{'alt'})
    {
        $asgroup = 1;
        $type = 0x42;
        $type |= 0x80 if ($self->{'neg'});
        $res = pack('C4', ($min << 4) + $max, $type, 2, 3);
        $names = {$self->{'id'} => 0};
        $oroff = 2;
        $index++;
    }
    
    $lacc = 0;
    foreach $r ($opts{'reverse'} ? reverse @{$self->{'child'}} : @{$self->{'child'}})
    {
        ($text, $sub, $subl) = @{$r->asTec(%opts)};
        if (defined $self->{'alt'})
        {
            if ($count)
            { 
                $res .= pack('C4', 0x11, 0x44, 0, length($res) / 4); 
                substr($res, $oroff, 1) = pack('C', (length($res) - $oroff - 2) / 4);
                $oroff = length($res) - 2;
                $index++;
            }
            else
            { $count = 1; }
            $res .= $text;
            $lacc = $subl if $subl > $lacc;
        }
        else
        {
            $res .= $text;
            $lacc += $subl;
        }
        foreach $k (keys %{$sub})
        { $names->{"$self->{'id'}/$k"} = $sub->{$k} + $index; }
        $index += length($text) / 4;
    }

    if ($asgroup)
    {
        $res .= pack('C4', ($min << 4) + $max, 0x43, 0, length($res) / 4);
        substr($res, 3, 1) = pack('C', length($res) / 4);
        substr($res, $oroff, 1) = pack('C', (length($res) - $oroff - 2) / 4);
    }

    return [$res, $names, $lacc * $max];
}

sub findfirst
{
    my ($self) = @_;
    my ($r, %res, @res1);
    
    foreach $r (@{$self->{'child'}})
    {
        @res1 = $r->findfirst();
        map {$res{$_} = 1} @res1;
        last if (!defined $self->{'alt'} && (!defined $r->base()->{'min'} || $r->base()->{'min'} != 0));
    }
    return (keys %res);
}

package Encode::UTR22::Regexp::classRef;

sub asTec
{
    my ($self, %opts) = @_;
    my ($class) = $self->{'owner'}{'classes'}{$self->{'name'}};
    my ($res, $temp, $wrap);
    my ($min, $max, $type);

    $min = defined $self->{'min'} ? $self->{'min'} : 1;
    $max = defined $self->{'max'} ? $self->{'max'} : 1;
    $type = 0x80 if ($self->{'neg'});

    return warn("No class defined for $self->{'name'}\n    in " . $self->as_error) unless defined $class;

    if ($self->{'id'} && $opts{'collect'})
    {
        $ind = {$self->{'id'} => 0};
    }
    else
    { $ind = {}; }
    
    if (scalar @{$class->{'elements'}} == 1)
    { 
        my ($val) = unpack($class->{'size'} eq 'bytes' ? 'C' : 'U', $class->{'elements'}[0]);
        $res .= pack('CCn', ($min << 4) + $max, ($val >> 16) | $type, $val & 0xFFFF);
    }
    else
    { $res .= pack('C2n', ($min << 4) + $max, 0x41 | $type, $class->asTecRef(%opts)); }
    
    return [$res, $ind, $max];
}

sub findfirst
{
    my ($self) = @_;
    my ($class) = $self->{'owner'}{'classes'}{$self->{'name'}};

    return @{$class->{'elements'}};
}

package Encode::UTR22::Regexp::contextRef;

sub base
{
    my ($self) = @_;
    my ($ref, $n);

    foreach $n (split('/', $self->{'name'}))
    {
        if ($ref)
        { $ref = $ref->{'named'}{$n}; }
        else
        { $ref = $self->{'owner'}{'contexts'}{$n}; }
        unless ($ref)
        {
            print STDERR "Can't find reference to $n in $self->{'name'} at line $self->{'line'}\n";
            return undef;
        }
        if ($ref->isa('Encode::UTR22::Regexp::contextRef'))
        { $ref = $self->{'owner'}{'contexts'}{$ref->{'name'}}; }
    }
    $ref;
}

sub asTec
{
    my ($self, %opts) = @_;
    my ($ref, $res, $ind, $temp, $len, $id, $resind);

    $ref = $self->base() || return ['', {}];

    $self->{'named'} = $ref->{'named'};

    if (defined $self->{'max'} || defined $self->{'min'})
    {
        $temp = bless {%$ref}, ref $ref;
        $temp->{'max'} = $self->{'max'} if defined $self->{'max'};
        $temp->{'min'} = $self->{'min'} if defined $self->{'min'};
        ($res, $ind, $len) = @{$temp->asTec(%opts)};
    } else
    { ($res, $ind, $len) = @{$ref->asTec(%opts)}; }

    $id = $self->{'id'} || $self->{'name'};
    $resind = {};
    foreach $n (keys %{$ind}) 
    { 
        my ($k) = $n;
        $n =~ s|^[^/]+|$id|o;
        $resind->{$n} = $ind->{$k};
    }
    
    return [$res, $resind, $len];
}


sub findfirst
{
    my ($self) = @_;
    my ($ref, $n);
    
    $ref = $self->base() || return ();
    
    return $ref->findfirst();
}    


package Encode::UTR22::Regexp::EOS;

sub asTec
{
    my ($self, %opts) = @_;
    my ($type);
    
    $type = 0x80 if ($self->{'neg'});
    return [pack('CCCC', 0x11, 0x46 | $type, 0, 0), {}, 0];
}

sub findfirst
{ return (); }

package Encode::UTR22::Regexp::class;

our %tecCount;

sub asTecRef
{
    my ($self, %opts) = @_;

    if (!defined $self->{'tecRef'}{$opts{'pass'}})
    { $self->{'tecRef'}{$opts{'pass'}} = $tecCount{$opts{'pass'}}++; }

    return $self->{'tecRef'}{$opts{'pass'}};
}


sub asTec
{
    my ($self, $asUni, %opts) = @_;
    my (@packing) = ('C', 'n', 'N');

#    return undef unless defined ($self->{'tecRef'}{$opts{'pass'}});
    return (pack("N$packing[$asUni]*", $#{$self->{'elements'}} + 1,
        sort {$a <=> $b} map {unpack($asUni ? 'U' : 'C', $_)} @{$self->{'elements'}}));
}

