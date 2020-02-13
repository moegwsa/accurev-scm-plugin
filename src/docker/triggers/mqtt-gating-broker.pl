use strict;
use warnings;
use Net::MQTT::Simple;
use XML::Simple;
use Fcntl qw(:flock :seek);

use Scalar::Util qw(looks_like_number);

use File::Basename;
use File::Spec::Functions qw(rel2abs);
use File::Spec;
use File::Log;

sub main {
	use Fcntl ':flock';
	open my $self, '<', $0 or die "Couldn't open self: $!";
	flock $self, LOCK_EX | LOCK_NB or die print "This script is already running";
    my $path = join("",dirname(rel2abs($0)),"/../logs/brokerLog.log");
    print "path is: $path \n"  ;
    my $log = File::Log->new ({
        debug           => 2,                   # Set the debug level
        logFileName     => $path,               # define the log filename
        logFileMode     => '>>',                # '>>' Append or '>' overwrite
        dateTimeStamp   => 1,                   # Timestamp log data entries
    });
	mqttListener($log);
	$log->msg(2,"Closing broker\b");
	$log->close();
}

sub mqttListener {
    my ($log) = @_;

    $log->msg(2, "Starting broker\n");
    print "Starting broker \n";
	# Attach broker to localhost MQTT instance
	my $mqtt = Net::MQTT::Simple->new("localhost");

	my $response;
	my $xmlInput = `accurev info -fx`;
	my $accurevInfo = XMLin($xmlInput);

	$mqtt->run(
	# Listen to every subtopic related to gatedStream
	"gatedStream/#" => sub {
		my ($topic, $response) = @_;
		my @info = split('/', $topic);
		# The response contains the URL for the build in Jenkins and the result
		my @content = split('\n', $response);
		$log->msg(2,"Received message on channel:  [$topic]\n");

		my $loginStatus = `accurev secinfo`;
    chomp ($loginStatus);
    if ($loginStatus eq "notauth") {
            $log->msg(2,"MQTT broker is not logged in, logging in with received credentials.\n");
						loginToAccurev($content[2], $content[3])
    }

		# Get staging_stream and transactionId from topic
		my $staging_stream = $info[1];
		my $transaction_num = $info[2];

		my $url = $content[0];

		# Need to set the result to one of three values.
		my $result = $content[1];  # [success | failed | warning]

		$log->msg(2,"Staging stream built: $staging_stream \n");
		$log->msg(2,"Transaction built: $transaction_num \n");
		$log->msg(2,"Jenkins url: $url \n");
		$log->msg(2,"Result: $result \n");

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
				my $principalAndComment = getPrincipalAndComment($staging_stream,$transaction_num);
				$log->msg(2, "principalAndComment is: ".$principalAndComment."\n");
				my $promote_result;
				if (not looks_like_number($principalAndComment)){
					$promote_result = system("accurev promote -s \"$staging_stream\" -d -t $transaction_num -c \"$principalAndComment\"");
				}

				# If promote failed, possibly became overlapped during external action
				if ($promote_result) {
					$result = 'warning';
					$tooltip = "Unable to promote transaction $transaction_num";
				}
			}

			$log->msg(2, "Result: $result\n");
			# Change the icon to the result
			system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon($result, $url, $tooltip)."\"");

			# Report the result (this must be the last accurev command before exiting the trigger)
			system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");
		}

		# On failure
		if ($result eq 'FAILURE') {

			$result = 'failed';
			my $tooltip = "Build failed for transaction $transaction_num";

			$log->msg(2, "Result: $result\n");

			# Change the icon to the result
			system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon($result, $url, $tooltip)."\"");

			# Report the result (this must be the last accurev command before exiting the trigger)
			system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");

			$log->msg(2, "Set $staging_stream to failed.\n");
		}
		# On warning
		if ($result eq 'UNSTABLE') {
            $result = 'warning';
            my $tooltip = "There were errors in transaction $transaction_num";

            $log->msg(2, "Result: $result\n");

            # Change the icon to the result
            system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon($result, $url, $tooltip)."\"");

            # Report the result (this must be the last accurev command before exiting the trigger)
            system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");

            $log->msg(2, "Set $staging_stream to failed.\n");
		}
	}
	);
}

sub loginToAccurev {
	my (@credentials) = @_;
	system("accurev login $credentials[0] $credentials[1]");
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

sub getPrincipalAndComment {
	my ($stream, $transaction_num) = @_;
	my $dir = dirname(rel2abs($0));
	my $file = "$dir/../temp/gated_input_file-$stream-$transaction_num.xml";
	if (-e $file ){
		print $file.": is found \n";
		open TIO, "<$file" or die "Can't open $file";
		my $xmlinput_raw;
		while (<TIO>){
			$_ =~ s/^\<\?xml version(.*)\?\>//i;
			$xmlinput_raw = ${xmlinput_raw}.$_;
		}
		close TIO;

		# populate array using XML::Simple routine
		my $xmlinput = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '');

		my $principal = $$xmlinput{'principal'}[0];
		my $comment = $$xmlinput{'comment'}[0];

		return $principal.": ".$comment; 
	}
	return -1;
}
&main;
