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

use lib dirname(__FILE__);
use AccurevUtils;
use Capture::Tiny "capture";

sub main {
    use Fcntl ':flock';
    open my $self, '<', $0 or die "Couldn't open self: $!";
    flock $self, LOCK_EX | LOCK_NB or die print "This script is already running";
    my $path = join("", dirname(rel2abs($0)), "/../logs/brokerLog.log");
    print "path is: $path \n";
    my $log = File::Log->new({
        debug         => 2,     # Set the debug level
        logFileName   => $path, # define the log filename
        logFileMode   => '>>',  # '>>' Append or '>' overwrite
        dateTimeStamp => 1,     # Timestamp log data entries
    });
    mqttListener($log);
    $log->msg(2, "Closing broker\b");
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
            $log->msg(2, "Received message on channel:  [$topic]\n");

            my $loginStatus = `accurev secinfo`;
            chomp($loginStatus);
            if ($loginStatus eq "notauth") {
                $log->msg(2, "MQTT broker is not logged in, logging in with received credentials.\n");
                loginToAccurev($content[2], $content[3])
            }

            # Get staging_stream and transactionId from topic
            my $staging_stream = $info[1];
            my $transaction_num = $info[2];

            my $url = $content[0];

            # Need to set the result to one of three values.
            my $result = $content[1]; # [success | failed | warning]

            $log->msg(2, "Staging stream built: $staging_stream \n");
            $log->msg(2, "Transaction built: $transaction_num \n");
            $log->msg(2, "Jenkins url: $url \n");
            $log->msg(2, "Result: $result \n");

            # On success...
            if ($result eq 'SUCCESS') {

                $result = 'success';
                my $tooltip = "Successfully built transaction $transaction_num";

                my $xml = `accurev show -s $staging_stream -fx streams`;

                my $xmlinput = XMLin($xml);
                my $streamType = $xmlinput->{stream}->{type};
                if ($streamType eq 'staging') {
                    # Unlock the staging stream
                    $log->msg(2,"received $result from staging stream");
                    system("accurev unlock -kf \"$staging_stream\"");

                    # Promote the changes
                    $log->msg(2,"stream is: $staging_stream and transaction number is $transaction_num");
                    my ($principalAndComment, $issue) = getPrincipalAndComment($staging_stream, $transaction_num);
                    $log->msg(2, "principalAndComment is: " . $principalAndComment . "\n");
                    my $promote_result;
                    if (not looks_like_number($principalAndComment)) {
                        $log->msg(2, "promoting $principalAndComment to $staging_stream");
                        my ($out, $err, $ext) = capture {
                            if(undef $issue && $issue ne 0){
                                $promote_result = system("accurev promote -s \"$staging_stream\" -d -t $transaction_num -c \"$principalAndComment\" -I $issue");
                            } else {
                                $promote_result = system("accurev promote -s \"$staging_stream\" -d -t $transaction_num -c \"$principalAndComment\"");
                            }
                        };
                        # If promote failed, possibly became overlapped during external action
                        if ($promote_result) {
                            $result = 'warning';
                            $err=~s/[\x00-\x1F]+/./g;
                            $tooltip = "Unable to promote transaction $transaction_num, due to $err";
                        }
                    }
                }
                $log->msg(2, "Result: $result\n");
                # Change the icon to the result
                system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"" . generateCustomIcon($result, $url, $tooltip) . "\"");

                # Report the result (this must be the last accurev command before exiting the trigger)
                system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");
            }

            # On failure
            if ($result eq 'FAILURE') {

                $result = 'failed';
                my $tooltip = "Build failed for transaction $transaction_num";

                $log->msg(2, "Result: $result\n");

                # Change the icon to the result
                system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"" . generateCustomIcon($result, $url, $tooltip) . "\"");

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
                system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"" . generateCustomIcon($result, $url, $tooltip) . "\"");

                # Report the result (this must be the last accurev command before exiting the trigger)
                system("accurev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");

                $log->msg(2, "Set $staging_stream to failed.\n");
            }
            if ($result eq 'ABORTED'){
                $result = 'failed';
                my $tooltip = "the build of transaction $transaction_num was aborted";

                $log->msg(2, "Result: $result\n");

                # Change the icon to the result
                system("accurev setproperty -r -s \"$staging_stream\" streamCustomIcon \"" . generateCustomIcon($result, $url, $tooltip) . "\"");

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

sub getPrincipalAndComment {
    my ($stream, $transaction_num) = @_;
    my $dir = dirname(rel2abs($0));
    my $file = "$dir/../temp/gated_input_file-$stream-$transaction_num.xml";
    #my $dir = "temp";
    #my $file = $dir."/gated_input_file-".$stream."-".$transaction_num.".xml";
    if (-e $file) {
        print $file . ": is found \n";
        open TIO, "<$file" or die "Can't open $file";
        my $xmlinput_raw;
        while (<TIO>) {
            $_ =~ s/^\<\?xml version(.*)\?\>//i;
            $xmlinput_raw = ${xmlinput_raw} . $_;
        }
        close TIO;

        unlink $file;
        # populate array using XML::Simple routine
        my $xmlinput = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '');

        my $principal = $$xmlinput{'principal'}[0];
        my $comment = $$xmlinput{'comment'}[0];
        my $issue = $$xmlinput{'changePackageID'}[0];
        $comment=~s/[\x00-\x1F]+/./g;

        return $principal . ": " . $comment, $issue;
    }
    return -1;
}
&main;
