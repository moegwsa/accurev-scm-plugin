package hudson.plugins.accurev.AccurevSCM

def l = namespace(lib.JenkinsTagLib)

['ACCUREV_TRANSACTION', 'ACCUREV_STREAM','ACCUREV_SERVER','ACCUREV_HOST'].each {name ->
    l.buildEnvVar(name: name) {
        raw(_("${name}.blurb"))
    }
}