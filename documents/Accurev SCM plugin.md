# Accurev SCM plugin

## Tools
- Github https://github.com/WiMills/accurev-scm-plugin
- Travis https://travis-ci.org/WiMills/accurev-scm-plugin
- Jitpack https://jitpack.io/#WiMills/accurev-scm-plugin
- Docker https://hub.docker.com/repository/docker/wimill/accurev

## Project structure
The project uses Maven as build tool, having its dependencies declared in the pom file.
The project code is split into
- java
  - hudson.plugins.accurev
  - jenkins.plugins.accurev
The views that are used in Jenkins are stored under ressources in folders correspondong to their java folders, e.g.
- ressources
  - hudson.plugins.accurev
  - jenkins.plugins.accurev

The hudson.plugins.accurev package contains files that are relevant to the Freestyle Projects and Pipeline Projects. They are used whenever Jenkins check out from Accurev. The package also contains the entrypoint for webhooks coming from Accurev, located in teh file AccurevStatus, and the response step to return result of run to Accurev.

The jenkins.plugins.accurev package contains the files used for Multibranch projects and the Accurev Step to use in a Jenkinsfile.

## Files of importance
### Hudson.plugins.accurev
AccurevSCM
- This one handles the checkout of files from Accurev. Determines the correct transaction to populate from, and populates the files on the agent, in the assigned workspace folder. Once it is done populating, it computes the changelog, with the transactions that has happened since previous run was done.
AccurevStatus
- Responsible for handling incoming webhooks from Accurev. Tries to parse the parameters and creates a scmhead event if there is a stream or depot match.
extensions/
- This folder contains the extensions that are used at checkout time. It is possible to create new extensions by extending from the AccurevSCMExtensions class

### Jenkins.plugins.accurev
AccurevSCMSource
- Handles the multibranch project types. The retrieve function checks whether there is an update on any of the streams that the multibranch project has. If there is a hit, the stream builds. The lightweight checkout that multibranch projects support for just checking out the Jenkinsfile is done in the createProbe function. The createProbe contains nothing, but requires the AccurevSCMFileSystemBuilder, and AccurevSCMFileSystem.
AccurevSCMFileSystem
- Is used to create a virtual filesystem, used to probe for Jenkinsfile without populating the entire workspace.
traits/
- This contains the traits that can be applied to the multibranch project. The traits are attached to the accurev extensions, meaning that it is possible to attach extensions to individual pipeline projects.
AccurevStep
- This creates the accurev step that can be used in a Jenkinsfile to check out from Accurev.

## References
The plugin was created by following https://github.com/jenkinsci/scm-api-plugin/blob/master/docs/implementation.adoc and drawing inspiration from the Git plugin for Jenkins.
Info about how to debug a plugin: https://wiki.jenkins.io/display/JENKINS/Plugin+tutorial#Plugintutorial-DebuggingaPlugin

## Traits
The traits currently available are
- Build items Discovery: This makes it possible to choose what build items you want to build, stream, workspace, snapshot, etc..
- Build with workspace: This creates an Accurev workspace at the Jenkins workspace location. If the workspace already exists, the workspace is moved to the new Jenkins workspace location.
- Top Stream Discovery: This is used if you want to create a multibranch project where your stream to discover from shouldn't be the depot stream. In a depot where you have 100+ streams, and you maybe only want 5 of them in a project, usage of this trait makes it a bit easier.
- Trigger children: When using this, if a stream is triggered, children streams are triggered as well. Use this trait with caution.
