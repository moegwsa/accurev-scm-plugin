use Win32::ServiceManager;
use Path::Class 'file';

my $dir = file(__FILE__)->parent->absolute;
my $sc = Win32::ServiceManager->new();

$sc->create_service(
  name => "AccurevMosquitoBroker",
  display => "Accurev Mosquito broker",
  description => "Handles incoming requests from Jenkins Build server regarding Gated Streams",
  use_perl => 1,
  command => $dir->file(qw(mosquitto-gating-broker.pl))
);
$sc->start_service("AccurevMosquitoBroker", {non_blocking => 0 });
