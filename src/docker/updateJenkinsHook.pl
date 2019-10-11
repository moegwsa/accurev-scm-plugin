use strict;
use warnings;

binmode STDOUT, ":utf8";
use utf8;
use JSON;

my $port = $ARGV[0];
my $host = $ARGV[1];


if((not defined $host) && (not defined $port)){
  print "Please specify a port and or host! \n";
  exit(0);
}

my $file = glob('~/accurev/storage/site_slice/triggers/jenkinsConfig.JSON');
my $json;
{
  local $/; #Enable 'slurp' mode
  open my $fh, "<", $file or die $!;
  $json = <$fh>;
  close $fh;
}
my $data = decode_json($json);
$data->{'config'}->{'port'} = $port;

if(defined $host){
  $data->{'config'}->{'host'} = $host;
}

my $json_text = encode_json($data);

open(my $fh, ">", $file);
print $fh $json_text;
close $fh;
