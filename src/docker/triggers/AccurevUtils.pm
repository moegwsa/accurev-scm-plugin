package AccurevUtils;

use warnings;
use strict;
use Exporter qw(import);
use File::Basename;
use File::Spec::Functions qw(rel2abs);
use File::Spec;

use constant {
    CREATED => 'created',
    UPDATED => 'updated',
    DELETED => 'deleted'
};


use Fcntl qw(:flock :seek);

our @ISA = qw(Exporter);
our @EXPORT = qw(generateCustomIcon log_trigger_error CREATED UPDATED DELETED);

# generate the streamCustomIcon xml
#  status should be a string with one of the allowed icon images: running, failed, success, warning
#  url is the url to open a browser on when you click on the icon
#  tooltip is the string to show as the tooltip when you hover over the icon
sub generateCustomIcon($$$) {
   my($xml);
   my($status, $url, $tooltip) = ($_[0], $_[1], $_[2]);
   $xml = "<streamicon>";
   if (length($status) gt 0) {
      $xml = $xml . "<image>" . $status . "</image>";
   }
   if (length($url) gt 0) {
      $xml = $xml . "<clickurl>" . $url . "</clickurl>";
   }
   if (length($tooltip) gt 0) {
      $xml = $xml . "<tooltip>" . $tooltip . "</tooltip>";
   }
   $xml = $xml . "</streamicon>";
   return $xml;
}

sub log_trigger_error {
   my $error = shift;

   my $path = dirname(rel2abs($0));
   my $trigger_log = "$path/../logs/trigger.log";
   open my $fh, '>>', $trigger_log;
   if ($fh) {
      flock($fh, LOCK_EX);
      seek($fh, 0, SEEK_END);
      my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime(time);
      my $timestamp = sprintf("%04d/%02d/%02d %02d:%02d:%02d", $year+1900, $mon+1, $mday, $hour, $min, $sec);
      print $fh "$timestamp $$ server_master_trig: $error\n";
      close $fh;
   }
}
