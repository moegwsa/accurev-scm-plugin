use strict;
use warnings;
use Net::MQTT::Simple;
use XML::Simple;
use Fcntl qw(:flock :seek);
use File::Spec::Functions qw(rel2abs);
use File::Basename;

sub main {
	use Fcntl ':flock';
	open my $self, '<', $0 or die "Couldn't open self: $!";
	flock $self, LOCK_EX | LOCK_NB or die print "This script is already running";
	mqttListener();

}

sub mqttListener() {

	#log_trigger_error("Starting broker\n");
	print "Starting broker\n";
	# Attach broker to localhost MQTT instance
	my $mqtt = Net::MQTT::Simple->new("localhost");

	my $response;
	my $xmlInput = `accurev info -fx`;
	my $accurevInfo = XMLin($xmlInput);

	$mqtt->run(
	# Listen to every subtopic related to gatedStream
	"gatedStream/#" => sub {
		my ($topic, $message) = @_;

		print "Received message on channel:  [$topic]\n";
		$response = $message;
		my @info = split('/', $topic);

		# Get staging_stream and transactionId from topic
		my $staging_stream = $info[1];
		my $transaction_num = $info[2];


		# The response contains the URL for the build in Jenkins and the result
		my @content = split('\n', $response);

		my $url = $content[0];


		# Need to set the result to one of three values.
		my $result = $content[1];  # [success | failed | warning]

		print "Staging stream built: $staging_stream \n";
		print "Transaction built: $transaction_num \n";
		print "Jenkins url: $url \n";
		print "Result: $result \n";
		# On success...
		if ($result eq 'SUCCESS') {

			$result = 'success';
			my $tooltip = "Successfully built transaction $transaction_num";


			my $xml = `accurev show -s $staging_stream -fx streams`;

			my $xmlinput = XMLin($xml);
			my $streamType = $xmlinput->{stream}->{type};
			if($streamType eq 'staging'){
				# Unlock the staging stream
				system("accurev unlock -kf \"$staging_stream\"");

				# Promote the changes
				my $promote_result = system("accurev promote -s \"$staging_stream\" -d -t $transaction_num");

				# If promote failed, possibly became overlapped during external action
				if ($promote_result) {
					$result = 'warning';
					$tooltip = "Unable to promote transaction $transaction_num";
				}
			}

			print "Result: $result\n";

			# Change the icon to the result
			system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon($result, $url, $tooltip)."\"");

			# Report the result (this must be the last accurev command before exiting the trigger)
			system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");
		}

		# On failure
		if ($result eq 'FAILURE') {

			$result = 'failed';
			my $tooltip = "Build failed for transaction $transaction_num";

			print "Result: $result\n";

			# Change the icon to the result
			system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon($result, $url, $tooltip)."\"");

			# Report the result (this must be the last accurev command before exiting the trigger)
			system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");

			print "Set $staging_stream to failed.\n"
		}
		# On warning
		if ($result eq 'UNSTABLE') {
            $result = 'warning';
            my $tooltip = "There were errors in transaction $transaction_num";

            print "Result: $result\n";

            # Change the icon to the result
            system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon($result, $url, $tooltip)."\"");

            # Report the result (this must be the last accurev command before exiting the trigger)
            system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");

            print "Set $staging_stream to failed.\n"
		}

		writeToLog($staging_stream, $transaction_num, $result, $url, $response);
	}
	);

}

sub writeToLog($$$) {
	my ($stream, $transaction_num, $result, $url, $response) = ($_[0], $_[1], $_[2], $_[3], $_[4]);

	my $path = dirname(rel2abs($0));
	my $file = "$path/../logs/brokerLog.log";

	my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime(time);
	my $timestamp = sprintf("%04d/%02d/%02d %02d:%02d:%02d", $year+1900, $mon+1, $mday, $hour, $min, $sec);

	open my $fh, '>>', $file;
	if($fh) {
		flock($fh, LOCK_EX);
		seek($fh, 0, SEEK_END);
		print $fh "$timestamp Jenkins response from $url\n";
		print $fh "Transaction number: $transaction_num\n";
		print $fh "Stream built: $stream\n";
		print $fh "Result: $result\n";
		#print $fh "Original response: $response \n";
		close $fh;
		print "Response logged";
	}
}

# generate the streamCustomIcon xml
#  status should be a string with one of the allowed icon images: running, failed, success, warning
#  url is the url to open a browser on when you click on the icon
#  tooltip is the string to show as the tooltip when you hover over the icon
sub generateCustomIcon($$$) {
   my ($xml);
   my ($status, $url, $tooltip) = ($_[0], $_[1], $_[2]);
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


&main;
