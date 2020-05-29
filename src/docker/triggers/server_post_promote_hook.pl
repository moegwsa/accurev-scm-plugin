#!/usr/bin/perl --

# @@ COPYRIGHT NOTICES @@
# Copyright (c) Micro Focus 2017
# All Rights Reserved


# Server Post Promote trigger script
# AccuRev post-promote trigger: server_post_promote.pl

# This trigger script will run on the server and can be used to call
# other scripts after a promote transaction has completed

# STEP 1 of 9:
# Make sure that the location of perl is correct in the first line of this
# script.

# STEP 2 of 9:
# Copy this trigger to the accurev bin directory on the server
# NOTE: If the server is running on Windows, run "pl2bat server_post_promote.pl"
# to convert this script into a batch file.
# Also make sure to run pl2bat anytime you modify this script.
# NOTE: If the server is running on unix, make sure to run:
# chmod +x server_post_promote.pl

# STEP 3 of 9:
# To enable the trigger:
# Unix:
# accurev mktrig -p <depot> server-post-promote-trig server_post_promote.pl
# Windows:
#
#

# STEP 4 of 9:
# Make sure to change the following to the actual path of accurev bin.
#
# Unix Example
 $::AccuRev = "/home/accurev-user/accurev/bin/accurev";
## Windows Example
#  $::AccuRevBin = "C:\\progra~1\\accurev\\bin";
#
# Make sure to uncomment and edit one of the above AccuRev bin locations


# STEP 5 of 9:
# Make sure to change the following to the actual path of the accurev client program
# Unix Example
 $::AccuRev = "/home/accurev-user/accurev/bin/accurev";
# Windows Example
#  $::AccuRev = "C:\\progra~1\\accurev\\bin\\accurev.exe";
#
# Make sure to uncomment and edit one of the above AccuRev bin locations

use strict;
use File::Basename;
use XML::Simple;
use File::Copy;
use File::Path qw(make_path);
use lib dirname (__FILE__);
use warnings;

use JenkinsHook;
use AccurevUtils;

sub main
{
    print "server_post_promote triggered\n";
    my ($file, $hook, $project, $stream);
    my ($comment, $comment_lines, @comment, $author, @files);
    my ($from_client_promote, $transaction_num, $transaction_time);
    my ($xmlinput, $xmlinput_raw, $file2, @elems, $depot, $principal, $elem_name);
    my ($changePackageID, @changePackageIDs);

    # standard input file
    $file = $ARGV[0];

    # read XML trigger input file
    $file2 = $ARGV[1];
    open TIO, "<$file2" or die "Can't open $file2";
    while (<TIO>){
        $xmlinput_raw = ${xmlinput_raw}.$_;
    }
    close TIO;

    # populate array using XML::Simple routine
    $xmlinput = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '');

    # set variables
    $hook = $$xmlinput{'hook'}[0];
    $principal = $$xmlinput{'principal'}[0];
    $depot = $$xmlinput{'depot'}[0];
    $stream = $$xmlinput{'stream1'}[0];
    $comment = $$xmlinput{'comment'}[0];
    $from_client_promote = $$xmlinput{'fromClientPromote'}[0];
    $transaction_num = $$xmlinput{'transNum'}[0];
    $transaction_time = $$xmlinput{'transTime'}[0];
    if ( $$xmlinput{'changePackages'}[0] ne "" ){
       foreach $changePackageID (@{$$xmlinput{'changePackages'}[0]{'changePackageID'}}){
          push (@changePackageIDs, $changePackageID);
       }
    }
    foreach $elem_name (@{$$xmlinput{'elemList'}[0]{'elem'}}) {
       push (@elems, $$elem_name{'content'});
    }

    $author = $principal;
    $project = $depot;
    @files = @elems;
    push (@comment, $comment);

    # STEP 6 of 9:
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
    # $ENV{'HOMEPATH'} = "\\Documents and Settings\\replace_with_username";
    #
    # STEP 2:
    # =======
    # ***This only needs to be done once during initial setup.***
    # Log the AccuRev user in by creating a permanent session token
    #  - Open a shell or command prompt
    #  - Set the home directory to the same values as above
    #    Unix:
    #       export HOME=/home/replace_with_username
    #    Winows:
    #       set HOMEDRIVE=c:
    #       set HOMEPATH=\Documents and Settings\replace_with_username
    #  - Login using the -n (non-expiring switch)
    #       accurev login -n <replace_with_username> <replace_with_password>
    #######################################################

    # Validate that the script user is logged in
    my $loginStatus = `$::AccuRev secinfo`;
    #my $loginStatus = system("$::AccuRev secinfo");
    chomp ($loginStatus);
    if ($loginStatus eq "notauth") {
            print "server_post_promote_trig: script user is not logged in.\n";
            exit(1);
    }

    # STEP 7 of 9:
    #   Call the update_ref.pl script to update your reference trees.
    #   Make sure that the path to the update_ref.pl exists in
    #   $::AccuRevBin or set the path below.  Keep in mind that update_ref.pl
    #   must run under an operating-system identity that has write permission
    #   to the reference-tree storage.
    #
    # Unix:
    # system("$::AccuRevBin/update_ref.pl $stream");
    #
    # Windows:
    #system("$::AccuRevBin\\update_ref $stream");

    #

    # STEP 8 of 9:
    #   If you are using the AccuRev/OpenTrack integration,
    #   uncomment the following line and make sure server_ot_promote.pl
    #   exists in $::AccuRevBin
    #
    # Unix:
    # system("$::AccuRevBin/server_ot_promote.pl", $file, $file2);
    #
    # Windows:
    # system("$::AccuRevBin\\server_ot_promote", $file, $file2);
	cacheInputFile($file2, $stream, $transaction_num, $principal); # cacheInputFile
	
	system("$::AccuRev setproperty -r -s \"$stream\" streamCustomIcon \"".generateCustomIcon("running", "", "Processing transaction $transaction_num")."\"");
    my $result = 'running';
    system("$::AccuRev setproperty -r -s \"$stream\" stagingStreamResult \"$result\"");
    notifyBuild(AccurevUtils->UPDATED, $stream, $depot, $transaction_num);

	# $::AccuRev = "C:\\progra~1\\accurev\\bin\\accurev.exe";
	

    # STEP 9 of 9:
    #   If you would like email notification of changes made
    #   uncomment the following line and make sure email_post_promote.pl (unix)
    #   or email_post_promote.bat (windows) exists in $::AccuRevBin.
    #   You should also edit email_post_promote(.pl)/(.bat) and customize it.
    #
    # Unix:
    # system("$::AccuRevBin/email_post_promote.pl", $file, $file2);
    #
    # Windows:
    # system("$::AccuRevBin\\email_post_promote", $file, $file2);

    # we're done, clean out the input file.
    open TIO, ">$file" or die "Can't open $file";
    close TIO;
    close STDERR;
    close STDIN;
    close STDOUT;
    exit (0);
}

&main();
