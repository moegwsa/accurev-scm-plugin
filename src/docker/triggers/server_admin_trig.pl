#!/usr/bin/perl --

# @@ COPYRIGHT NOTICES @@
# Copyright (c) Micro Focus 2017
# All Rights Reserved


# ################ INTRODUCTION
#
# repository-wide server-side pre-operation trigger: server_admin_trig.pl
#
# This trigger script runs on the AccuRev Server machine, before certain
# user commands are executed. This is a repository-wide trigger: it runs
# no matter what depot (if any) the user command applies to.
#
# If the script exits with a zero value (SUCCESS), the user command
# proceeds; if the script exits with a nonzero value (FAILURE), the
# command is aborted and a message created by this script is displayed to
# the user.
#
# As of Release 7.1, this script is called only when the following commands
# are issued by an AccuRev client program (CLI or GUI):
#
#   addmember, authmethod, chdepot, chpasswd, chref, chslice, chstream,
#   chuser, chws, lock, lsacl, mkdepot, mkgroup, mkreplica, mkstream,
#   mktrig, mkuser, mkws, reactivate, remove, rmmember, rmproperty,
#   rmreplica, rmtrig, rmws, setacl, setproperty, trace-event, unlock
#   replica_sync ("accurev replica sync" command)
#   defcomp (incl, incldo, excl, and clear commands)
#   write_schema (Save command on Schema Editor tab in AccuRev GUI)
#   read_schema (Get schema information from the server)
#
#   In a replication environment, this script runs on the master server when
#   triggered by any of the above commands. EXCEPTION: for an rmreplica command,
#   this script runs on the replica server.
#
# NOTE: This trigger is intended to replace the old trigger
# 'server_all_trig'.  This new trigger will ONLY run if the
# server_all_trig trigger does NOT exist in the site_slice/triggers
# directory. If you are migrating from the server_all_trig trigger to this
# new server_admin_trig trigger, please remove the files:
# .../site_slice/triggers/server_all_trig*
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

use XML::Simple;
use Data::Dumper;
use strict;
use File::Copy;

use File::Basename;
use lib dirname (__FILE__);
use JenkinsHook;
use JenkinsHook('updateCrumb');

sub main
{
    my ($file, $xmlinput_raw, $xmlinput);
    my ($hook, $command, $principal, $ip, $objectType, $objectName, $streamType);
    my ($stream1, $stream2, $stream3, $depot, $comment);
    my ($fromClientPromote, $changePackagePromote);
    my (@elems, $elem_name);
    my (@groups, $group);
    my (@cmdlist);
    my ($admingrp, $ws_owner, %admin_stream, %basis_stream_deny, %replica_depot_deny);
    my ($user, $newKind, $newName);
    my ($result);

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
    # Unix Example
    $::AccuRev = "/home/accurev-user/accurev/bin/accurev";
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
    $ENV{'HOME'} = "/home/accurev_user";
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
    chomp ($loginStatus);
    if ($loginStatus eq "notauth") {
            $file = $ARGV[0];
            open TIO, ">$file" or die "Can't open $file";
            print TIO "server_admin_trig: script user is not logged in.\n";
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
    # $admin_stream{"replace_with_admin_stream"} = 1;
    # $admin_stream{"replace_with_admin_stream"} = 1;
    # $admin_stream{"replace_with_admin_stream"} = 1;
    ####################################################### CUSTOMIZE ME
    # Specify the streams that cannot by used as a basis
    # (backing) stream in "mkstream" commands, except by
    # administrators. There are no minimum/maximum limits.
    ######################################################
    # $basis_stream_deny{"replace_with_basis_stream_to_deny"} = 1;
    # $basis_stream_deny{"replace_with_basis_stream_to_deny"} = 1;
    # $basis_stream_deny{"replace_with_basis_stream_to_deny"} = 1;

    ####################################################### CUSTOMIZE ME
    # Specify the depots that cannot be replicated or synchronized.
    # There are no minimum/maximum limits.
    ######################################################
    # $replica_depot_deny{"replace_with_depot_to_deny"} = 1;
    # $replica_depot_deny{"replace_with_depot_to_deny"} = 1;
    # $replica_depot_deny{"replace_with_depot_to_deny"} = 1;

    ######################################################
    # Set variables for use by trigger script
    #
    # For a given command, only some of these variables
    # will get meaningful values. See the comments
    # in the individual command-validation sections below.
    ######################################################


    # read trigger input file
    $file = $ARGV[0];

    open TIO, "<$file" or die "Can't open $file";
    while (<TIO>){
        $xmlinput_raw = ${xmlinput_raw}.$_;
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
    $streamType = $$xmlinput{'streamType'}[0];
    $user = $$xmlinput{'user'}[0];
    $newKind = $$xmlinput{'newKind'}[0];
    $newName = $$xmlinput{'newName'}[0];
    $fromClientPromote = $$xmlinput{'fromClientPromote'}[0];
    $changePackagePromote = $$xmlinput{'changePackagePromote'}[0];
    $comment = $$xmlinput{'comment'}[0];
    foreach $elem_name (@{$$xmlinput{'groups'}[0]{'group'}}) {
       push (@groups, $elem_name);
    }
    foreach $elem_name (@{$$xmlinput{'elemList'}[0]{'elem'}}) {
       push (@elems, $elem_name);
    }

	print "server_admin_trig: $command was here\n";
	print "Stream changed: $stream1 \n";


    ###
    ### prevent recursion by quitting early for certain commands
    ###

    if ($command eq "ismember") {
        exit(0);
    }

    ###################################################### CUSTOMIZE ME
    # Set additional variables, for compatibility with
    # existing 'server_all_trig' script code
    #
    # If you copy existing server_all_trig code into this
    # file, uncomment the following variable definitions.
    # Some of the new 'server_admin_trig' variable names are
    # slightly different from the old 'server_all_trig'
    # variable names.
    ######################################################

    # my ($cmd, $stream, $obj_type, $obj_name);
    # $cmd = $command;
    # $obj_type = $objectType;
    # $obj_name = $objectName;
    # if ($cmd eq "lock" || $cmd eq "unlock" || $cmd eq "chws" || $cmd eq "chstream" ) {
    #   $stream = $stream1;
    # } elsif ($cmd eq "mkstream") {
    #   $stream = $stream2;
    # }

    ######################################################

    # prepare to overwrite the input file with trigger script output
    open TIO, ">$file" or die "Can't open $file";

    ####
    #### Validation for MKUSER, CHDEPOT, ...
    ####

    # list of user commands to which following validation code applies
    @cmdlist = qw/addmember chdepot chref chslice lsacl mkdepot
                  mkgroup mkuser mktrig rmmember rmtrig setacl
                  setproperty rmproperty read_schema write_schema /;

    # is the user command in the above list?
    if (grep $_ eq $command, @cmdlist) {

    # at this point, the following variables will have meaningful values:
    #     $hook        Trigger name
    #     $command     AccuRev command being run
    #     $principal   Username of person invoking command
    #	  $user		   Set for addmember, rmmember, and ismember
    #	  $group	   Set for addmember, rmmember, and ismember
    #     $ip          IP address of AccuRev client machine

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the above commands.
    ######################################################

    # NOTE on CHDEPOT:
    # To fully disable this command, make sure the depot's base stream
    # is specified in the admin_stream hash. Otherwise,
    # chdepot will allow the base stream to change, and will only
    # prevent the name of the depot itself from changing.

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can run the command
        if ( `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Execution of '$command' command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for MKUSER, CHDEPOT, ...

    ####
    #### Validation for CHWS command
    ####

    if ($command eq "chws") {
    # NOTE: when it renames and/or re-parents a workspace,
    # the user command "accurev chws" causes the server_admin_trig script
    # to fire twice:
    #   first firing: $command is set to "chws"
    #   second firing: $command is set to "chstream"

    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Workspace stream name
    #     $stream2      Backing stream of workspace ($stream1)
    #     $stream3      New workspace stream name (if changing workspace name)

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the CHWS command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only the workspace owner or an administrator
        # can make a change to a workspace
        my $wsdata = `$::AccuRev show -fx -a wspaces`;
        my $wsHref = XMLin($wsdata, forcearray => 1, suppressempty => '');
        my $wspaces = $wsHref->{Element};
        my $userWspace = undef;
        foreach my $elemHref (@$wspaces) {
        	next unless ($stream1 eq $elemHref->{Name});
        	$userWspace = $elemHref;
        	last;
        }
        if ("$userWspace->{user_name}" ne $principal and `$::AccuRev ismember $principal "$admingrp"` == 0) {
            print TIO "Cannot 'chws' workspace '$stream1'.\n";
            print TIO "This workspace belongs to another user, or\n";
            print TIO "You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # #end of EXAMPLE VALIDATION



    # # no problems, allow command to proceed
    close TIO;
    exit(0);
    }

    #### end of validation for CHWS command


    ####
    #### Validation for CHUSER command
    ####

    if ($command eq "chuser") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $user         Username of user entry being changed
    #     $newKind      New user kind (dispatch or cm), if changing
    #     $newName      New user name, if changing

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the CHUSER command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can make a
        # change to someone else's username
        if ($newName ne "" and $principal ne $user and `$::AccuRev ismember $principal "$admingrp"` == 0) {
            print TIO "Cannot 'chuser' user '$user'.\n";
            print TIO "This username belongs to another user, and\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            exit(1);
        }
        # only a user listed as an administrator can make a
        # change to someone's user kind for licensing purposes
        if ($newKind ne "" and `$::AccuRev ismember $principal "$admingrp"` == 0) {
            print TIO "Cannot 'chuser' license kind.\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for CHUSER command


    ####
    #### Validation for CHPASSWD command
    ####

    if ($command eq "chpasswd") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $user         Username of user entry being changed

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the CHPASSWD command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can make a
        # change to someone else's password
        if ($principal ne $user and `$::AccuRev ismember $principal "$admingrp"` == 0) {
            print TIO "$principal cannot 'chpasswd' ${user}'s password.\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for CHPASSWD command


    ####
    #### Validation for MKWS command
    ####

    if ($command eq "mkws") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Workspace stream name
    #     $stream2      Backing stream of workspace ($stream1)

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the MKWS command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can make a workspace
        # based on streams in the "basis_stream_deny" list
        if ( defined($basis_stream_deny{$stream2}) and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Execution of 'mkws' command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for MKWS command


    ####
    #### Validation for LOCK command
    ####

    if ($command eq "lock") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      name of stream being locked

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the LOCK command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can lock a stream
        # in the "admin_stream" list

        if ( defined($admin_stream{$stream1}) and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Locking a stream identified as an 'admin stream' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for LOCK command

    ####
    #### Validation for UNLOCK command
    ####

    if ($command eq "unlock") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      name of stream being unlocked

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the UNLOCK command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can unlock a stream
        # in the "admin_stream" list

        if ( defined($admin_stream{$stream1}) and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Unlocking a stream identified as an 'admin stream' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for UNLOCK command


    ####
    #### Validation for DEFCOMP command
    #### (incl, incldo, and excl,
    ####

    if ($command eq "defcomp") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      name of stream with defcomp being applied

    # NOTE: The DEFCOMP command is automatically run when a user creates
    #       a "Pick and Choose" workspace.
    #       If the sample logic below is changed to block DEFCOMP operations
    #       on all streams instead of just "admin" streams, it will interfere
    #       with the creation of "Pick and Choose" workspaces.
    #       Please contact AccuRev support before making changes
    #       to the sample logic below.

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the DEFCOMP command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can run defcomp on a stream
        # in the "admin_stream" list

        if ( defined($admin_stream{$stream1}) and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Running defcomp on a stream identified as an 'admin stream' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION
		notifyBuild($command, $stream1, $depot, $principal);
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for DEFCOMP command

    ####
    #### Validation for CHSTREAM command
    ####

    if ($command eq "chstream") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      name of stream being changed
    #     $stream2      Backing stream of $stream1
    #     $stream3      New stream name (if changing stream name)

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the CHSTREAM command.
    ######################################################

        # EXAMPLE VALIDATION:
        # only a user listed as an administrator can change a stream
        # in the "admin_stream" list
        #
        if ( defined($admin_stream{$stream1}) and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Modifying a stream identified as an 'admin stream' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
             exit(1);
         }
        # end of EXAMPLE VALIDATION
		# Rebase stream
		if($stream3 eq '') {
			notifyBuild($command, $stream1, $depot, $principal);
		# Rename stream
		}else{
			notifyBuild($command, $stream3, $depot, $principal);
		}


        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for CHSTREAM command

    ####
    #### Validation for MKSTREAM command
    ####

    if ($command eq "mkstream") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $stream1      Name of new stream
    #     $stream2      Backing stream of new stream ($stream1)
    #     $streamTypeType of stream being created: regular, passthru, snapshot


    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the MKSTREAM command.
    ######################################################

	# EXAMPLE VALIDATION 1:
	# only a user listed as an administrator can create a new snapshot

        if ($streamType eq "snapshot" and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
	    print TIO "Making a snapshot disallowed:\n";
	    print TIO "server_admin_trig: Only a member of the group $admingrp can create snapshots.\n";
            close TIO;
	    exit(1);
	    }

        # end of EXAMPLE VALIDATION 1

        # EXAMPLE VALIDATION 2:
        # only a user listed as an administrator can create a new stream
        # based on an existing stream in the "basis_stream_deny" list

        if ( defined($basis_stream_deny{$stream2}) and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Basing a new stream on existing stream '$stream2' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION 2

        notifyBuild($command, $stream1, $depot, $principal);
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for MKSTREAM command

    ####
    #### Validation for REMOVE command
    ####

    if ($command eq "remove") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $objectType   Object type:
    #                     1=Reference Tree
    #                     2=Workspace
    #                     3=Stream
    #                     5=User
    #                     6=Group
	#		    7=Sessions
    #     $objectName   Object name

    # NOTE 1: Removing a workspace actually removes two types of objects:
    #         Workspace (objectType 2) and Stream (objectType 3).
    #         Be sure to block removals of both types, to avoid
    #         the situation where the workspace is removed, but not the
    #         workspace stream (or vice-versa).
    #
    # NOTE 2: The command "accurev rmws" uses the same internal code path as
    #         "accurev remove wspace".
    #         Therefore, rmws will not show up as a command in this trigger, but
    #         will instead show up as "remove" with an object type of "2" (workspace).

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the REMOVE command.
    ######################################################

        # EXAMPLE VALIDATION 1:
        # only a user listed as an administrator can remove
        # a stream named "stream_123"

        if ( ($objectType eq "3" and $objectName eq "stream_123")
                 and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Removal of stream '$objectName' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
          }
        # end of EXAMPLE VALIDATION 1

        # EXAMPLE VALIDATION 2:
        # only a user listed as an administrator can remove
        # someone else's workspace

        if ( ($objectType eq "2" and $objectName !~ /(.*)_($principal)$/)
                 and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Execution of 'remove' command for someone else's workspace disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION 2

        # EXAMPLE VALIDATION 3:
        # only a user listed as an administrator can remove
        # someone else's sessions

        if ( ($objectType eq "7" and $objectName !~ /(.*)_($principal)$/)
                 and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Execution of 'remove' command for someone else's sessions disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION 3
        notifyBuild($command, $objectName, $depot, $principal);
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for REMOVE command

    ####
    #### Validation for REACTIVATE command
    ####

    if ($command eq "reactivate") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $objectType   Object type:
    #                     1=Reference Tree
    #                     2=Workspace
    #                     3=Stream
    #                     5=User
    #                     6=Group
    #     $objectName   Object name

    # NOTE: Reactivating a workspace actually reactivates two types of objects:
    #       Workspace (objectType 2) and Stream (objectType 3).
    #       Be sure to block reactivation of both types, to avoid
    #       the situation where the workspace is reactivated, but not the
    #       workspace stream (or vice-versa).

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the REACTIVATE command.
    ######################################################

        # EXAMPLE VALIDATION 1:
        # only a user listed as an administrator can reactivate
        # a stream named "stream_123"

        if ( ($objectType eq "3" and $objectName eq "stream_123")
                 and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Reactivating stream '$objectName' disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
          }
        # end of EXAMPLE VALIDATION 1

        # EXAMPLE VALIDATION 2:
        # only a user listed as an administrator can reactivate a user

        if ( $objectType eq "5" and `$::AccuRev ismember $principal "$admingrp"` == 0 ) {
            print TIO "Reactivating a user disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }
        # end of EXAMPLE VALIDATION 2
        notifyBuild($command, $objectName, $depot, $principal);
        
        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }

    #### end of validation for REACTIVATE command

    ####
    #### Validation for REPLICA_SYNC command
    #### ("accurev replica sync" command)
    #### This trigger fires on the master server
    ####

    if ($command eq "replica_sync") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $depot        Depot (project) name.

    # Add the name of the depot or depots to be disallowed to the %replica_depot_deny hash.

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the REPLICA_SYNC command.
    ######################################################

        # a depot in the "depot deny" list defined above cannot be synchronized
        if ( defined($replica_depot_deny{$depot}) ) {
            print TIO "Execution of 'replica sync' command disallowed:\n";
            print TIO "server_admin_trig: The $depot depot may not be synchronized.\n";
            close TIO;
            exit(1);
        }

        # only a user listed as an administrator can run the command
        $result = `$::AccuRev ismember $principal "$admingrp"`;
        if ( $result == "0" ) {
            print TIO "Execution of replica sync command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            print TIO "The principal is $principal\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for REPLICA_SYNC command

    ####
    #### Validation for MKREPLICA command
    #### This trigger fires on the master server
    ####

    if ($command eq "mkreplica") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $depot        Depot (project) name.

    # Add the name of the depot or depots to be disallowed to the %replica_depot_deny hash.

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the MKREPLICA command.
    ######################################################

        # a depot in the "depot deny" list defined above cannot be replicated
        if ( defined($replica_depot_deny{$depot}) ) {
            print TIO "Execution of 'mkreplica' command disallowed:\n";
            print TIO "server_admin_trig: The $depot depot may not be replicated.\n";
            close TIO;
            exit(1);
        }

        # only a user listed as an administrator can run the command
        $result = `$::AccuRev ismember $principal "$admingrp"`;
        if ( $result == "0" ) {
            print TIO "Execution of mkreplica command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            print TIO "The principal is $principal\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for MKREPLICA command

    ####
    #### Validation for RMREPLICA command
    #### This trigger fires on the replica server, not the master server
    ####

    if ($command eq "rmreplica") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine
    #     $depot        Depot (project) name.

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the RMREPLICA command.
    ######################################################

        # only a user listed as an administrator can run the command
        $result = `$::AccuRev ismember $principal "$admingrp"`;
        if ( $result == "0" ) {
            print TIO "Execution of rmreplica command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for RMREPLICA command

    ####
    #### Validation for AUTHMETHOD command
    ####

    # NOTE: this function is called only on an attempt to set
    # the user-authentication method. It is not called for a
    # request to display the current method.

    if ($command eq "authmethod") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the AUTHMETHOD command.
    ######################################################

        # # only a user listed as an administrator can run the command
        $result = `$::AccuRev ismember $principal "$admingrp"`;
        if ( $result == "0" ) {
            print TIO "Execution of authmethod command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for AUTHMETHOD command

    ####
    #### Validation for TRACE-EVENT command
    ####

    if ($command eq "trace-event") {
    # at this point, the following variables will have meaningful values:
    #     $hook         Trigger name
    #     $command      AccuRev command being run
    #     $principal    Username of person invoking command
    #     $ip           IP address of AccuRev client machine

    ###################################################### CUSTOMIZE ME
    #### Add to (or replace) the example code below to
    #### implement validation for the TRACE-EVENT command.
    ######################################################

        # only a user listed as an administrator can run the command
        $result = `$::AccuRev ismember $principal "$admingrp"`;
        if ( $result == "0" ) {
            print TIO "Execution of trace-event command disallowed:\n";
            print TIO "server_admin_trig: You are not in the $admingrp group.\n";
            close TIO;
            exit(1);
        }

        # no problems, allow command to proceed
        close TIO;
        exit(0);
    }
    #### end of validation for TRACE_EVENT command

close TIO;
exit(0);
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

- change the name of the file to "server_admin_trig" (no suffix)
- run the command "chmod +x server_admin_trig"

WINDOWS:

- run the command "PL2BAT server_admin_trig.pl" to convert this script
into a batch file, 'server_admin_trig.bat'. The PL2BAT command is part
of the Perl distribution.

NOTE: When you wish to modify this script, we recommend that you NOT
edit the batch file directly. Instead, edit this file (.pl suffix), then
run PL2BAT again to recreate the Windows batch file (.bat suffix).
