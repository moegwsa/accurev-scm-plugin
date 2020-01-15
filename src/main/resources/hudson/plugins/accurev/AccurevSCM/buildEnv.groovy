package hudson.plugins.accurev.AccurevSCM

def l = namespace(lib.JenkinsTagLib)

// TODO handle GitSCMExtension.populateEnvironmentVariables somehow, say by optionally including GitSCMExtension/buildEnv.groovy; though GIT_{COMMITTER,AUTHOR}_{NAME,EMAIL} are only overridden by UserIdentity
['ACCUREV_TRANSACTION', 'ACCUREV_STREAM'].each {name ->
    l.buildEnvVar(name: name) {
        raw(_("${name}.blurb"))
    }
}