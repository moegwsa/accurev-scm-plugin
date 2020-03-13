#!/usr/bin/perl --

# @@ COPYRIGHT NOTICES @@
# Copyright (c) Micro Focus 2017
# All Rights Reserved

# ################ INTRODUCTION
#
# repository-wide server-side trigger: server_master_trig.pl
#
# This trigger script runs on the AccuRev Server machine, before or after certain
# user commands are executed. This is a repository-wide trigger: it runs
# no matter what depot (if any) the user command applies to.
#
# For preop commands, if the script exits with a zero value (SUCCESS), the user command
# proceeds; if the script exits with a nonzero value (FAILURE), the
# command is aborted and a message created by this script is displayed to
# the user.
#
# ################ CUSTOMIZING THIS SCRIPT
#
# Find the several "CUSTOMIZE ME" comments in the Perl code below,
# and make the changes appropriate for your AccuRev installation.
#
# ################ INSTALLING THIS SCRIPT
#
# See "INSTALLATION INSTRUCTIONS" at the end of this file.

################ START OF TRIGGER SCRIPT CODE

use warnings;
use XML::Simple;
use Data::Dumper;
use strict;
use LWP::UserAgent;
use File::Spec::Functions qw(rel2abs);
use File::Basename;
use Fcntl qw(:flock :seek);
#use warnings;
use File::Copy;
use File::Basename;

sub main
{
  print "Server_master_trig script called, via normal print \n";
  log_trigger_error("server_master_trig script called via log_trig\n");

   my ($file, $xmlinput_raw, $xmlinput);
   my ($command);
   my ($pathArgs);
   my ($pathXML);
   my ($file2);
   # read trigger input file
   $file = $ARGV[0];
   #$xmlinput_raw = "";
   open TIO, "<$file" or die "Can't open $file";
   print "$file \n";
   while (<TIO>){
       $_ =~ s/^\<\?xml version(.*)\?\>//i;
       $xmlinput_raw = ${xmlinput_raw}.$_;
   }
   close TIO;

   # populate array using XML::Simple routine
   $xmlinput = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '', keyattr =>  {'+id'});
   # ################
   # The server_master_trig trigger is called by AccuRev as either a preop or postop trigger
   # The following lists the commands supported by the server_master_trig trigger
   #
   # Command		  Type	  Description
   # ===========================================================================================
   # streamEvent	postop  Called after any event which affects an Event Stream's contents
   # gatingAction postop  Called after promotion into a staging stream
   # cpkAdjust		postop  Called after an operation affects a change package (add / remove element)
   # cpkPost		  postop  Called after any promote against a change package
   # newIssue		  preop   Called before creating a new issue
   # modifyIssue	preop   Called before modifying an existing issue
   # queryIssue		preop   Called before running an issue query
   # ################

   # set variables
   $command = $$xmlinput{'name'};
   log_trigger_error("Command issued: $command");
   if ($command eq "streamEvent"){
      runStreamEvent($xmlinput);
      exit(0);
   } elsif ($command eq "gatingAction"){
      runGatingAction($xmlinput);
      exit(0);
   } elsif ($command eq "cpkAdjust"){
      runCpkAdjust($xmlinput);
      exit(0);
   } elsif ($command eq "cpkPost"){
      runCpkPost($xmlinput);
      exit(0);
   } elsif ($command eq "newIssue"){
      runNewIssue($xmlinput);
      exit(0);
   } elsif ($command eq "modifyIssue"){
      runModifyIssue($xmlinput);
      exit(0);
   } elsif ($command eq "queryIssue"){
      runQueryIssue($xmlinput);
      exit(0);
   } else {
      # unknown command type
      exit(0);
   }
}

sub runStreamEvent{
   my $xmlinput = shift(@_);
   # Uncomment the below to see a dump of the XML input file (shows up in trigger.log)
   # print Dumper($xmlinput);

   # Uncomment the below to pass the Stream Event to the Kando Server
   #postKandoEvent($xmlinput);
   exit(0);
}

sub runCpkAdjust{
   my $xmlinput = shift(@_);
   # Uncomment the below to see a dump of the XML input file (shows up in trigger.log)
   # print Dumper($xmlinput);
   exit(0);
}

sub runCpkPost{
   my $xmlinput = shift(@_);
   # Uncomment the below to see a dump of the XML input file (shows up in trigger.log)
   # print Dumper($xmlinput);
   exit(0);
}

sub runNewIssue{
   my $xmlinput = shift(@_);
   # Uncomment the below to see a dump of the XML input file (shows up in trigger.log)
   # print Dumper($xmlinput);
   exit(0);
}

sub runModifyIssue{
   my $xmlinput = shift(@_);
   # Uncomment the below to see a dump of the XML input file (shows up in trigger.log)
   # print Dumper($xmlinput);
   exit(0);
}

sub runQueryIssue{
   my $xmlinput = shift(@_);
   # Uncomment the below to see a dump of the XML input file (shows up in trigger.log)
   # print Dumper($xmlinput);
   exit(0);
}

sub postKandoEvent{
   my $xmlinput = shift(@_);
   my ($file, $xmlinput_raw);
   my ($command, $depotName, $depotID, $triggeringTrans, $eventStreams);
   my ($action_name, $action_id, $bridgeURI, $serverID);

   $depotName = $$xmlinput{'depot'}[0]{'name'};
   $depotID = $$xmlinput{'depot'}[0]{'id'};
   $triggeringTrans = $$xmlinput{'triggeringTrans'}[0]{'id'};
   foreach my $eventStream (@{$$xmlinput{'eventStreams'}[0]{'stream'}}){
     if (length($eventStreams) > 0){
       $eventStreams .= "," . $$eventStream{'id'};
     } else {
        $eventStreams =  $$eventStream{'id'};
     }
   }
   foreach my $stream (@{$$xmlinput{'streams'}[0]{'stream'}}){
      if ($$stream{'action'} eq "destination"){
         $action_name = $$stream{'name'};
         $action_id = $$stream{'id'};
      }
   }

   # Read in the Kando Config
   $file = "triggers/Kando.xml";
   $xmlinput_raw = "";
   open TIO, "<$file" or die "Can't open $file";
   while (<TIO>){
       $xmlinput_raw = ${xmlinput_raw}.$_;
   }
   close TIO;

   # populate array using XML::Simple routine
   $xmlinput = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '', keyattr =>  {'+id'});

   $bridgeURI = $$xmlinput{'bridge_uri'}[0];
   $serverID = $$xmlinput{'server_id'}[0];

   my $ua = LWP::UserAgent->new();
   my $urlstring = sprintf("%s/server/%d/depot/%s/streams/%s?operation=export&dest_stream=%d&dest_stream_name=%s&transaction=%s", $bridgeURI, $serverID, $depotName, $eventStreams, $action_id, $action_name, $triggeringTrans);
   print "$urlstring\n";
   my $response = $ua->get($urlstring);
   if ($response->is_success) {
      print "Retrieved " .  length($response->decoded_content) .  " bytes of data.";
   } else {
      print "Error: " . $response->status_line;
   }
   exit(0);
}

sub runGatingAction{
   my $xmlinput = shift(@_);

   # set variables
   my $depot = $$xmlinput{'depot'}[0];
   my $staging_stream = $$xmlinput{'stagingStream'}[0];
   my $staging_stream_num = $$xmlinput{'stagingStreamNum'}[0];
   my $gated_stream = $$xmlinput{'gatedStream'}[0];
   my $gated_stream_num = $$xmlinput{'gatedStreamNum'}[0];
   my $transaction_num = $$xmlinput{'transNum'}[0];
   my $transaction_time = $$xmlinput{'transTime'}[0];
   my $backing_change = $$xmlinput{'backing_change'}[0];
   #log_trigger_error("in the gate\n");

   ####################################################### CUSTOMIZE ME
   # Change the following to the actual path of AccuRev.
   # The default (uncommented) logic below is configured
   # for Windows with an AccuRev bin location of:
   # C:\Program Files\AccuRev\bin
   #
   # If this script is installed on a Windows server:
   #  1. Edit the Windows example code accordingly
   #
   # If this script is installed on a Unix server:
   #  1. Comment out the Windows example code
   #  2. Uncomment the Unix example code
   #  3. Edit the Unix example code accordingly
   #######################################################
   # Unix
   use Cwd            qw( abs_path );
   use File::Basename qw( dirname );
   log_trigger_error(glob("~/accurev/bin/accurev"));
   $::AccuRev = glob("~/accurev/bin/accurev");
   #
   # Windows Example
   # $::AccuRev = qq("C:\\Program Files\\AccuRev\\bin\\accurev.exe");


   ####################################################### CUSTOMIZE ME
   # Script user setup:
   # STEP 1:
   # =======
   # Configure the path to the home directory for the
   # AccuRev user that the commands within this script will
   # run as. (This is where the login session token will be
   # stored)
   #
   # The default (uncommented) logic below is configured
   # for Windows.
   #
   # If this script is running on Windows:
   #  1. Edit the 2 Windows example code lines accordingly
   #
   # If this script is installed on a Unix server:
   #  1. Comment out the 2 Windows example code lines
   #  2. Uncomment the 1 Unix example code line
   #  3. Edit the 1 Unix example code line accordingly
   #
   # Unix Example
   # $ENV{'HOME'} = "/home/replace_with_username";
   #
   # Windows Example
   # $ENV{'HOMEDRIVE'} = "c:";
   # $ENV{'HOMEPATH'} = "\\Users\\replace_with_username";


   #
   # STEP 2:
   # =======
   # ***This only needs to be done once during initial setup.***
   # Log the AccuRev user in by creating a permanent session token
   #  - Open a shell or command prompt
   #  - Set the home directory to the same values as above
   #    Unix:
   #       export HOME=/home/replace_with_username
   #    Windows:
   #       set HOMEDRIVE=c:
   #       set HOMEPATH=\Documents and Settings\replace_with_username
   #  - Login using the -n (non-expiring switch)
   #       accurev login -n <replace_with_username> <replace_with_password>
   #######################################################
   #log_trigger_error("Set environment values");
   # Validate we have a valid path to the client
   unless (-e $::AccuRev) {
     log_trigger_error("AccuRev '$::AccuRev' is not a valid path");
     exit(1);
   }
	#log_trigger_error("Validated path successfully");
   # Validate that the script user is logged in
   my $loginStatus = `$::AccuRev secinfo`;
   chomp ($loginStatus);
   log_trigger_error("Chomped loginstatus");
   if ($loginStatus eq "notauth") {
     log_trigger_error("Script user not logged in (accurev secinfo returned '$loginStatus').  Please create a permanent session token (accurev login -n <username> <password>)");
     exit(1);
   } elsif ($loginStatus ne "anyuser" && $loginStatus ne "authuser") {
     log_trigger_error("secinfo command failed, unable to determine if script user is logged in");
     exit(1);
   }

   log_trigger_error("Passed initial checking");

   # Check for overlapped files before starting
   my $stat_output = `$::AccuRev stat -s \"$staging_stream\" -o`;
   chomp( my @overlapped = $stat_output =~ /\G(.*\n|.+)/g );
   if (scalar @overlapped) {
      system("$::AccuRev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon("warning", "", "Overlapped files require merge before promote")."\"");
      system("$::AccuRev setproperty -r -s \"$staging_stream\" stagingStreamResult \"warning\"");
      log_trigger_error("Overlap files found.");
	  exit(1);
   }

    log_trigger_error("No overlap");

    # Lock promotes from the staging stream
    system("$::AccuRev lock -kf \"$staging_stream\"");

    # Change the icon to running
    system("$::AccuRev setproperty -r -s \"$staging_stream\" streamCustomIcon \"".generateCustomIcon("running", "", "Processing transaction $transaction_num")."\"");

    ####################################################### CUSTOMIZE ME
    ##### START: CUSTOM EXTERNAL ACTION #####


    # If backing changed, use current time basis, otherwise populate up to transaction
    my $trn_arg = $backing_change eq 'true' ? 'now' : $transaction_num;

    # Populate the files associated with this transaction
    # system("$::AccuRev pop -v \"$staging_stream\" -t $trn_arg -O -R -L . .");

    use Socket;
    # Fetch docker host IP adress through Docker network, located at host.docker.internal
  	my $host = inet_ntoa(inet_aton("host.docker.internal"));
    # Get the port, standard from docker-compose file is set to 8081. Run changeJenkinsUrl PORT_NUM to chagne
    my $port = $ENV{'JENKINS_PORT'};

    binmode STDOUT, ":utf8";
    use utf8;
    use JSON;

    my $json;
      {
        local $/; #Enable 'slurp' mode
        my $file = "triggers/jenkinsConfig.JSON";
        open my $fh, "<", $file or die $!;
        $json = <$fh>;
        close $fh;
      }
      my $jenkinsConfig = decode_json($json);

      my $jPort = $jenkinsConfig->{'config'}->{'port'};
      my $jHost = $jenkinsConfig->{'config'}->{'host'};

    if($jPort ne ''){
      $port = $jPort;
    }

    if($jHost ne ''){
      $host = $jHost;
    }

    print "Port that should receive message: $port\n";
    my $url ="http://$host:$port/jenkins/accurev/notifyCommit/";

	print "Hook was triggered on stream: $staging_stream - Transaction number: $transaction_num \n";
	print "Sent to: $url \n";

	my $ua = LWP::UserAgent->new;
	# Set timeout for post calls to 10 seconds.
	$ua->timeout(10);
	my $xmlInput = `$::AccuRev info -fx`;
	my $accurevInfo = XMLin($xmlInput);


	# WHEN NOT TESTING ON LOCALHOST, USE $accurevInfo->{serverName} FOR HOST
	# Create a post call to the Jenkins server with the information regarding the stream that was promoted from
	my $res = $ua->post($url, {'host' => 'localhost', 'port' => $accurevInfo->{serverPort}, 'streams' => $staging_stream, 'transaction' => $transaction_num, 'principal' => 'gatedStreamPrincipal'});

	use HTTP::Status ();

	# The useragent can have a timeout if two requests are send too fast - Find a way to solve
	if ($res->is_error) {
		log_trigger_error($res->code);
		log_trigger_error($res->message);
		if($res->code == HTTP::Status::HTTP_REQUEST_TIMEOUT) {
			log_trigger_error("We hit a timeout");
		}
	}

	# Set stream property to running, if it is not set before server_master_trig.pl script closes, Accurev will go into an error state where no triggers can be triggered
	my $result = 'running';
	system("$::AccuRev setproperty -r -s \"$staging_stream\" stagingStreamResult \"$result\"");

    # Remove the input file when done
    unlink $ARGV[0] or warn "Could not unlink $ARGV[0]: $!";
	log_trigger_error("Unlinked file, exiting.");
    exit(0);
}

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

# run main routine
&main();

__END__

################ END OF TRIGGER SCRIPT CODE


################ INSTALLATION INSTRUCTIONS

Perform all the following steps on the AccuRev Server machine.

1. Make sure that the machine has a full installation of Perl, including
both the interpreter and the standard library modules. In particular,
this script uses the module XML::Simple. If necessary, you can get this
module from the Comprehensive Perl Archive Network (www.cpan.org).

Note: be sure to use XML::Simple, not the similarly named XML::Simpler.

2. (Unix only) Make sure that the first line of this file correctly
lists the directory where the Perl interpreter resides.

3. Create subdirectory 'triggers' in the site_slice directory within the
AccuRev installation area.

4. Copy this file to the 'triggers' directory. The existence of file
'server_admin_trig' in the 'triggers' directory makes the trigger
active.

5. Make the trigger script in the 'triggers' directory executable:

Unix:

- change the name of the file to "server_master_trig" (no suffix)
- run the command "chmod +x server_master_trig"

WINDOWS:

- run the command "PL2BAT server_admin_trig.pl" to convert this script
into a batch file, 'server_master_trig.bat'. The PL2BAT command is part
of the Perl distribution.

NOTE: When you wish to modify this script, we recommend that you NOT
edit the batch file directly. Instead, edit this file (.pl suffix), then
run PL2BAT again to recreate the Windows batch file (.bat suffix).
