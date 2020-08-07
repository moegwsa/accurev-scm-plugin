#!/usr/bin/perl

# @@ COPYRIGHT NOTICES @@
# Copyright (c) 1995-2007 AccuRev, Inc.
# All Rights Reserved
# Please read the up-to-date COPYRIGHT file at the top of the source tree

# ################ INTRODUCTION
# 
# per-depot server-side pre-operation trigger: server_preop_trig.pl
# 
# This trigger script runs on the AccuRev Server machine, before certain
# user commands are executed. This is a per-depot trigger: it runs only
# for commands that target a particular depot. (See the INSTALLATION
# INSTRUCTIONS below for details.)
# 
# If the script exits with a zero value (SUCCESS), the user command
# proceeds; if the script exits with a nonzero value (FAILURE), the
# command is aborted and a message created by this script is displayed to
# the user.
# 
# As of Release 3.5, this script is called only when the following commands
# are issued by an AccuRev client program (CLI or GUI):
# 
#   add, keep, promote, purge, co (also anchor), defunct
#
#
# ################ CUSTOMIZING THIS SCRIPT
# 
# Find the several "CUSTOMIZE ME" comments in the Perl code below,
# and make the changes appropriate for your AccuRev installation.
# 
# NOTE: The sample logic below tests whether or not a user is in
# a particular GROUP. The syntax for testing group membership is:
# 
#    accurev ismember <user> <group>
# 
#  This command returns:
#    1  (if the user is in the specified group)
#    0  (if the user is NOT in the specified group)
# 
# 
# ################ INSTALLING THIS SCRIPT
# 
# See "INSTALLATION INSTRUCTIONS" at the end of this file.

################ START OF TRIGGER SCRIPT CODE

use warnings;
use XML::Simple;
use Data::Dumper;
use strict;

use File::Copy;
use File::Path qw(make_path);

sub main
{
    my ($file, $file_output, $xmlinput_raw, $xmlinput);
    my ($hook, $command, $principal, $ip,$objectType, $objectName);
    my ($stream1, $stream2, $stream3, $depot, $comment,$issue_num);
    my ($fromClientPromote, $changePackageID, @changePackageIDs);
    my (@elems, $elem_name);
    my ($admingrp, %admin_stream);
    my ($user, $newKind, $newName, %elem_name);
    my ( $xmlinputHier, $xml_elem );
    my ( $xml_hierType, $file_output );
    my ( %hierInfo, %name, $elemId, $count );

    ####################################################### CUSTOMIZE ME
    # Change the following to the actual path of AccuRev. 
    #######################################################
    $::AccuRev = "/home/accurev-user/accurev/bin/accurev";
    # Windows
	# vasvcs01 + vasvcs02
    #$::AccuRev = "C:\\progra~1\\accurev\\bin\\accurev.exe";

    ####################################################### CUSTOMIZE ME
    # Script user setup:
    # STEP 1:
    # =======
    # Configure the path to the home directory for the
    # AccuRev user that the commands within this script will
    # run as. (This is where the login session token will be
    # stored)
    #
    # Windows Example
    # $ENV{'HOMEDRIVE'} = "c:";
    # $ENV{'HOMEPATH'} = "\\Documents and Settings\\replace_with_username";
    #$ENV{'HOMEDRIVE'} = "c:";
    #$ENV{'HOMEPATH'} = "\\";
    $ENV{'HOME'} = "/home/accurev_user";

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
    chomp ($loginStatus);
    if ($loginStatus eq "notauth") {
            $file = $ARGV[0];
            open TIO, ">$file" or die "Can't open $file";
            print TIO "server_preop_trig: script user is not logged in.\n";
            close TIO;
            exit(1);
    }
    
    ####################################################### CUSTOMIZE ME
    # Specify the AccuRev Group name of your Administrators
    # Edit this value accordingly.
    # To grant Administrator access to a user, add them 
    # to this AccuRev group.
    #######################################################
    $admingrp = "Admin";

    # The following hashes are used in the EXAMPLE VALIDATION code below.

    ####################################################### CUSTOMIZE ME
    # Specify the streams that can only be modified by
    # administrators. There are no minimum/maximum limits.
    #######################################################
    $admin_stream{"replace_with_admin_stream"} = 1;

    ######################################################
    # Set variables for use by trigger script
    #
    # For a given command, only some of these variables
    # will get meaningful values. See the comments
    # in the individual command-validation sections below.
    ######################################################

    # read trigger input file
    $file = $ARGV[0];
    $file_output = "preop_input";

    open TIO, "<$file" or die "Can't open $file";
    while (<TIO>){
        $xmlinput_raw = ${xmlinput_raw}.$_;
    }
    close TIO;

    open TIO, ">$file_output" or die "Can't open $file_output";
    #normalize the ip address data so it works everywhere
    my @lines= split(/\n/,$xmlinput_raw),;
    my $line;
    foreach $line (@lines) {
       unless( $line =~ /<ip.*ip>/i){
            print TIO "$line\n";
       }
    } 
    close TIO;

    # populate array using XML::Simple routine
    $xmlinput = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '');

    # set variables
    $hook = $$xmlinput{'hook'}[0];
    $command = $$xmlinput{'command'}[0];
    $principal = $$xmlinput{'principal'}[0];
    $ip = $$xmlinput{'ip'}[0];
    $stream1 = $$xmlinput{'stream1'}[0];
    $stream2 = $$xmlinput{'stream2'}[0];
    $stream3 = $$xmlinput{'stream3'}[0];
    $depot = $$xmlinput{'depot'}[0];
    $objectType = $$xmlinput{'objectType'}[0];
    $objectName = $$xmlinput{'objectName'}[0];
    $user = $$xmlinput{'user'}[0];
    $newKind = $$xmlinput{'newKind'}[0];
    $newName = $$xmlinput{'newName'}[0];
    $fromClientPromote = $$xmlinput{'fromClientPromote'}[0];
    if ( $$xmlinput{'changePackagePromote'}[0] ne "" ){
       foreach $changePackageID (@{$$xmlinput{'changePackagePromote'}[0]{'changePackageID'}}){
          push (@changePackageIDs, $changePackageID);
       }
    }
    $comment = $$xmlinput{'comment'}[0];
    foreach $elem_name (@{$$xmlinput{'elemList'}[0]{'elem'}}) {
       push (@elems, $elem_name);
    }
	# for cpkadd and cpkremove
    $issue_num = $$xmlinput{'issueNum'}[0];
	
    # The following is used in add command validation of 
    # exclusive file locking: xmlinputHier, xml_elem, xml_hierType, file_output
    $xmlinputHier = XMLin($xmlinput_raw, forcearray => 1, suppressempty => '', keyattr => { elem => '+count' });

    $xml_elem = $$xmlinputHier{'elements'}[0];
	  $xml_hierType = $$xmlinputHier{'hierType'}[0];
    $file_output = $$xmlinputHier{'output_file'}[0];

    # prepare to overwrite the input file with trigger script output
    open TIO, ">$file" or die "Can't open $file";

    # overwrite comment if autopromote from mqqt-gating-broker
    # change accurev_user to the admin user used for autopromote by the accurev server.
    if (verifyComment($comment)  and $principal eq "accurev_user"){
        my ($_principal, $_comment) = split /: /, $comment;
        print "new comment is: $_comment \n";
        print "new principal is: $_principal \n";
        open TIO1, ">>:encoding(UTF-8)", $file_output or die "Can't open $file_output";
        print TIO1 "<comment>$_comment</comment>";
        close TIO1;
        open TIO1, ">>:encoding(UTF-8)", $file or die "Can't open $file";
        print TIO1 "<principal>$_principal</principal>";
        close TIO1;
    } else {
        print "no need to change comment and principal";
    }

    cacheInputFile($file_output, $stream1, $principal); # cacheInputFile
    cacheInputFile($file, $stream1, $principal); # cacheInputFile

    print "Proccessing: $command \n";

    ######################################################
    ####
    #### Validation for ADD command
    ####
    if ($command eq "add") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Workspace stream to which elements are being added
    #     $stream2      Backing stream of workspace ($stream1)
    #     $depot        Depot name
    #     $comment      Comment
    #     @elems        Element list
      
        # only an administrator can run the ADD without entering a comment
        if ( $comment eq "" and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "\nPlease add a comment for this add operation\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for ADD command

	
    ######################################################
    ####
    #### Validation for KEEP command
    ####
    if ($command eq "keep") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Workspace stream to which elements are being kept
    #     $stream2      Backing stream of workspace ($stream1)
    #     $depot        Depot name
    #     $comment      Comment
    #     @elems        Element list

        # only an administrator can run the KEEP without entering a comment
        if ( $comment eq "" and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "\nPlease add a comment for this keep operation\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for KEEP command

	
    ######################################################
    ####
    #### Validation for PROMOTE command
    ####
    if ($command eq "promote") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Stream promoting FROM (source stream)
    #     $stream2      Stream promoting TO (destination stream)
    #     $depot        Depot name
    #     $fromClientPromote    Data passed from pre-promote-trig script
    #     $changePackagePromote Change-package issue number specd by user
    #     $comment              Comment
    #     @elems                Element list

        # only an administrator can run the PROMOTE without entering a comment
        if ( length $comment < 5 and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            #print TIO "Empty comments for 'keep' command disallowed:\n";
            print TIO "\nPlease add a resonable comment for this promote operation\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for PROMOTE command

	
    ######################################################
    ####
    #### Validation for PURGE command
    ####
    if ($command eq "purge") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Stream from which versions are being purged
    #     $depot        Depot name
    #     $fromClientPromote    Data passed from pre-promote-trig script
    #     @elems                Element list

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for PURGE command

	
    ######################################################
    ####
    #### Validation for CO (also Anchor) command
    ####
    if ($command eq "co") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Workspace stream to which elements are being co'd
    #     $stream2      Backing stream of workspace ($stream1)
    #     $depot        Depot name
    #     @elems        Element list

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for CO command

    ######################################################
    ####
    #### Validation for DEFUNCT command
    ####
    if ($command eq "defunct") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Workspace stream in which elements are being defuncted
    #     $stream2      Backing stream of workspace ($stream1)
    #     $depot        Depot name
    #     @elems        Element list

	
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for DEFUNCT command

	
    ######################################################
    ####
    #### Validation for CPKADD command
    ####
    if ($command eq "cpkadd") {
	# at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $issue_num  Issue Number
    #     @elems        Element list	

	
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for CPKADD command

	
    ######################################################
    ####
    #### Validation for CPKREMOVE command
    ####
    if ($command eq "cpkremove") {
	# at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $issue_num  Issue Number
    #     @elems        Element list		

	
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for CPKREMOVEcommand

	
close TIO;
exit(0);
}

# run main routine
&main();

sub verifyComment{

    my ($content) = @_;
    if ($content =~/\A[a-zA-Z]+:+[A-Za-z0-9\s]+\z/){
        return 1;
    }else{
        return 0;
    }
}

sub cacheInputFile {
    my ($file, $stream) = @_;
    # copy XML trigger input file to new location
    my $dir = "temp";
    my $filecopy = $dir."/gated_input_file-".$stream.".xml";
    eval { make_path($dir) };
    if ($@) {
        print "Couldn't create $dir: $@";
    }
    print "copying file: $filecopy";
    copy($file, $filecopy);
}

__END__

################ END OF TRIGGER SCRIPT CODE


################ INSTALLATION INSTRUCTIONS

Perform all the following steps on the AccuRev client machine.

1. Make sure that the machine has a full installation of Perl, including
both the interpreter and the standard library modules. In particular,
this script uses the module XML::Simple. If necessary, you can get this
module from the Comprehensive Perl Archive Network (www.cpan.org).

Note: be sure to use XML::Simple, not the similarly named XML::Simpler.

2. (Unix only) Make sure that the first line of this file correctly
lists the directory where the Perl interpreter resides.

3. Decide which depot is to be controlled by this trigger script. If you
wish to control several depots with the same script, you must perform
the remaining steps separately for each depot.

    The examples in following steps assume the depot to be controlled
    is named "amber".

4. Create subdirectory 'triggers' in the depot directory. Examples:

    Unix:       /var/accurev/storage/depots/amber/triggers
    Windows:    C:\Program Files\AccuRev\storage\depots\amber\triggers

5. Copy this file to the 'triggers' directory. The existence of file
'server_preop_trig' in the 'triggers' directory of a depot makes the
trigger active for that depot.

6. Make the trigger script in the 'triggers' directory executable:

Unix:

- Change the name of the file to "server_preop_trig" (no suffix)
- Run the command "chmod +x server_preop_trig"

WINDOWS:

- Run the command "PL2BAT server_preop_trig.pl" to convert this script
into a batch file, 'server_preop_trig.bat'. The PL2BAT command is part
of the Perl distribution.

NOTE: When you wish to modify this script, we recommend that you NOT
edit the batch file directly. Instead, edit this file (.pl suffix), then
run PL2BAT again to recreate the Windows batch file (.bat suffix).

7. Check the full pathname to the 'triggers' directory. If this pathname
includes one or more SPACE characters, e.g.:

    C:\Program Files\AccuRev\storage\depots\amber\triggers
    
... then you must do either Fix A or Fix B below to ensure the
correct execution of the batch file:

Fix A
-----

You must change the AccuRev database, so that the pathname stored for
the depot directory is a DOS short name.

- Run "accurev show slices" to find the depot directory pathname. The
relevant line output might be:

    Slice#  Location
     ...
    23      C:/Program Files/AccuRev/storage/depots/amber

- Run "accurev chslice" to revise this spec in the database. For this
example, the command is:

    accurev chslice -s 23 -l C:/Progra~1/AccuRev/storage/depots/amber

Fix B
-----

- Edit the "server_preop_trig.bat" file

- Remove the double-quotes from the "%0" on all lines that invoke the
Perl interpreter. For example, change this line:

  perl -x -S "%0" %1 %2 %3 %4 %5 %6 %7 %8 %9

    ... to this ...

  perl -x -S %0 %1 %2 %3 %4 %5 %6 %7 %8 %9

NOTE: you must perform Fix B each time you recreate the batch file with
PL2BAT.
