package hudson.plugins.accurev.ServerRemoteConfig

import lib.CredentialsTagLib
import lib.FormTagLib

f = namespace(FormTagLib)
c = namespace(CredentialsTagLib)

f.entry(title:_("Host"), field:"host") {
    f.textbox(default:"")
}

f.entry(title:_("Port"), field:"port"){
    f.textbox(default:"")
}

f.entry(title:_("Credentials"), field:"credentialsId") {
    c.select(onchange="""{
            var self = this.targetElement ? this.targetElement : this;
            var r = findPreviousFormItem(self,'host');
            r.onchange(r);
            self = null;
            r = null;
    }""" /* workaround for JENKINS-19124 */)
}
f.entry {
    div(align:"right") {
        input (type:"button", value:_("Add Server"), class:"repeatable-add show-if-last")
        input (type:"button", value:_("Delete Server"), class:"repeatable-delete show-if-not-only")
    }
}
