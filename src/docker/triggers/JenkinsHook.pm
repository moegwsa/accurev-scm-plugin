package JenkinsHook;

binmode STDOUT, ":utf8";
use warnings;
use strict;
use Exporter qw(import);
use Scalar::Util qw(looks_like_number);

use File::Copy;
use File::Path qw(make_path);
use experimental 'smartmatch';

use utf8;
use JSON qw//;
use JSON;
use Socket;
use LWP::UserAgent;
use URI;
use XML::Simple;

our @ISA = qw(Exporter);
our @EXPORT = qw(notifyBuild copyInputFile);

our @EXPORT_OK = qw(updateCrumb);

sub notifyBuild {
	my (@parameters) = @_;
	my $command = $parameters[0];
	my $stream = $parameters[1];
	my $depot = $parameters[2];
	my $transaction_num = $parameters[3];
	my $principal = $parameters[4];

	if(not looks_like_number($parameters[3])){
		$principal=$parameters[3];
	}
	
	print "Triggered stream: $stream - Cause: $command\n";

	my $url = "localhost:5050";
	my $crumbRequestField = "";
	my $crumb;
	my $jenkinsConfigFile = 'triggers/jenkinsConfig.json';

	if (-e $jenkinsConfigFile) {
		print "Jenkins configuration file found.\n";
		my ($urlFromFile, $crumbFromFile, $crumbRequestFieldFromFile) = readJenkinsConfigFile($jenkinsConfigFile);
		$url = $urlFromFile;
		$crumb = $crumbFromFile;
		$crumbRequestField = $crumbRequestFieldFromFile;

		if(not defined $crumb) {
			print "No crumb detected, obtaining crumb.\n";
			my ($crumbUpdated, $crumbRequestFieldUpdated) = updateCrumb($url);
			print "Updating Jenkins configuration file with newly obtained crumb.\n";
			updateJenkinsConfigFile($jenkinsConfigFile, $crumbUpdated, $crumbRequestFieldUpdated);
			$crumb = $crumbUpdated;
            print "crumb is $crumb \n";
			$crumbRequestField = $crumbRequestFieldUpdated;
		}
	} else {
		print "No Jenkins configuration file found, defaulting to localhost.\n";
	}
  
	my $urlToJenkins ="http://$url/accurev/notifyCommit/";
    print "Attempting to notify $urlToJenkins \n";
	my $userAgent = LWP::UserAgent->new;
  
	# Set timeout for post calls to 10 seconds.
	$userAgent->timeout(10);
	$userAgent->default_header($crumbRequestField => $crumb);
	my $xmlInput = `accurev info -fx`;
	my $accurevInfo = XMLin($xmlInput);
	print "Parsing input command to Reason \n";
    my $reason = parseCommandToReason($command);

	print "Triggering Stream on Jenkins. By $reason because of $command \n";
	# WHEN NOT TESTING ON LOCALHOST, USE $accurevInfo->{serverName} FOR HOST
	# Create a post call to the Jenkins server with the information regarding the stream that was promoted from
	if(!$command eq "gatingAction" || !$command eq "postPromote") {
		if($principal eq '') {
			$principal = "gatingActionPrincipal";
		}
		print "Notifying for: $urlToJenkins for other than postPromote and gatingAction \n";
		my $response = $userAgent->post($urlToJenkins, {
		    'host' => $accurevInfo->{serverName},
		    'port' => $accurevInfo->{serverPort},
		    'streams' => $stream,
		    'principal' => $principal,
		    'reason' => $reason
		});
		if ($response->is_error) {
            print "cannot notify build because \n";
            print $response->code . "\n";
            print $response->message . "\n";
        }
		if(!messageSucceeded($response->status_line)) {
			print "Invalid crumb, fetching new \n";
			my ($crumbUpdated, $crumbRequestFieldUpdated) = updateCrumb($url);
			updateJenkinsConfigFile($jenkinsConfigFile, $crumbUpdated, $crumbRequestFieldUpdated);
			print "Trying to trigger stream again. \n";
			$userAgent->default_headers->header($crumbRequestFieldUpdated => $crumbUpdated);
			$userAgent->post($urlToJenkins, {'host' => $accurevInfo->{serverName},
                'port' => $accurevInfo->{serverPort},
                'streams' => $stream,
                'principal' => $principal,
                'reason' => $reason
			});
		}
	}else{
	    print "Notifying for: $urlToJenkins for postPromote and gatingAction \n";
		my $response = $userAgent->post($urlToJenkins, {
		    'host' => $accurevInfo->{serverName},
		    'port' => $accurevInfo->{serverPort},
		    'streams' => $stream,
		    'transaction' => $transaction_num,
		    'principal' => $principal,
		    'reason' => $reason
		});
		if(!messageSucceeded($response->status_line)) {
			print "Invalid crumb, fetching new. \n";
			my ($crumbUpdated, $crumbRequestFieldUpdated) = updateCrumb($url);
			updateJenkinsConfigFile($jenkinsConfigFile, $crumbUpdated, $crumbRequestFieldUpdated);
			print "Trying to trigger stream again. \n";
			$userAgent->default_headers->header($crumbRequestFieldUpdated => $crumbUpdated);
			$userAgent->post($urlToJenkins, {
			    'host' => $accurevInfo->{serverName},
			    'port' => $accurevInfo->{serverPort},
			    'streams' => $stream,
			    'transaction' => $transaction_num,
			    'principal' => $principal,
			    'reason' => $reason
			});
		}
	}

}

sub messageSucceeded {
	my ($response) = @_;
	if(index($response, "200 OK") != -1) {
		print "Message succeeded";
		return 1;
	}
	return 0;
}

sub updateCrumb {
	my ($url) = @_;
	my $urlToJenkinsApi ="http://$url/crumbIssuer/api/json";
    print "$urlToJenkinsApi \n";
	my $json = JSON->new->utf8;
	my $userAgent = LWP::UserAgent->new;
	my $response = $userAgent->get($urlToJenkinsApi);
	if ($response->is_error) {
        print "cannot obtain crumb because \n";
        print $response->code . "\n";
        print $response->message . "\n";
    } else {
	    my $responseInJson = $json->decode($response->decoded_content);
	    return ($responseInJson->{'crumb'}, $responseInJson->{'crumbRequestField'});
    }
}

sub readJenkinsConfigFile {
	my ($jenkinsConfigFile) = @_;
	my $json;
	{
		local $/; #Enable 'slurp' mode
		open my $fh, "<", $jenkinsConfigFile or die $!;
		$json = <$fh>;
		close $fh;
	}
	my $jenkinsConfig = decode_json($json);

	my $jenkinsUrl = $jenkinsConfig->{'config'}->{'url'};
	my $crumb = $jenkinsConfig->{'config'}->{'authentication'}->{'crumb'};
	my $crumbRequestField = $jenkinsConfig->{'config'}->{'authentication'}->{'crumbRequestField'};

	return $jenkinsUrl, $crumb, $crumbRequestField;
}

sub updateJenkinsConfigFile {
	my ($jenkinsConfigFile, $crumb, $crumbRequestField) = @_;

	my $jenkinsConfigJson;
	{
		local $/; #Enable 'slurp' mode
		open my $fh, "<", $jenkinsConfigFile or die $!;
		$jenkinsConfigJson = <$fh>;
		close $fh;
	}
	my $jenkinsConfigJsonDecoded = decode_json($jenkinsConfigJson);

	$jenkinsConfigJsonDecoded->{'config'}->{'authentication'}->{'crumb'} = $crumb;
	$jenkinsConfigJsonDecoded->{'config'}->{'authentication'}->{'crumbRequestField'} = $crumbRequestField;

	my $json = JSON->new->utf8;
	$json = $json->pretty([1]);

	my $jenkinsConfigUpdated = $json->encode($jenkinsConfigJsonDecoded);

	open(my $fh, ">", $jenkinsConfigFile);
	print $fh $jenkinsConfigUpdated;
	close $fh;
}

# we don't wanna process workspaces and keep for now
sub parseCommandToReason{
  my ($command) = @_;
  my $reason;
  if($command ~~ ["mkdepot" , "mkstream","reactivated","chstream"] ){ #chstream as rename
    $reason="created";
  }elsif($command ~~ ["rmdepot" , "rmstream"] ){
    $reason="deleted";
  }elsif($command ~~ ["defcomp", "gatingAction", "promote"] ){ #chstream as rebase
    $reason="updated";
  }else{
    $reason="unknown";
  }
  
  return $reason;
}

sub copyInputFile {
	my ($file, $stream, $transaction_num) = @_;
	# copy XML trigger input file to new location
    my $dir = "temp";
    my $filecopy = $dir."\\gated_input_file-".$stream."-".$transaction_num.".xml";
    eval { make_path($dir) };
    if ($@) {
       print "Couldn't create $dir: $@";
    }
    copy($file, $filecopy);
}

1;
