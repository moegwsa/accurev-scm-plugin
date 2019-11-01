package hudson.plugins.accurev.StreamSpec

import lib.FormTagLib

f = namespace(FormTagLib)

f.entry(title:_("Depot"), field:"depot"){
    f.textbox(default:"")
}

f.entry(title:_("Stream specifier (blank for 'any')"), field:"name") {
    f.textbox(default:"")
}

f.entry {
    div(align:"right") {
        input (type:"button", value:_("Add Stream"), class:"repeatable-add show-if-last")
        input (type:"button", value:_("Delete Stream"), class:"repeatable-delete")
    }
}
