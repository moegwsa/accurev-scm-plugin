# accurev-scm-plugin

# CURRENTLY IN DEVELOPMENT - DO NOT USE IN JENKINS !


The Accurev SCM plugin can be used in
- Freestyle projects
- Pipeline projects
- Multibranch projects

Used to integrate Accurev with Jenkins.

Requires Accurev client https://github.com/WiMills/accurev-client-plugin to run.

### Use in Freestyle projects
1. Choose Accurev as SCM
2. Fill out the Host and Port of the Accurev server, and choose credentials. These will be used to access Accurev.
3. Choose the Depot to be accessed and a Stream specifier. More than one Depot / Stream can be targeted by clicking "Add Stream".
4. Under Builder Triggers, choose whether Accurev is allowed to remote trigger the build by checking "Accurev hook trigger for SCMPolling".


### Use in pipeline projects:
1. In Pipeline -> Definition choose Pipeline script from SCM.
2. Choose Accurev as SCM.
3. Fill out the Host and Port of the Accurev server, and choose credentials. These will be used to access Accurev.
4. Choose the Depot to be accessed and a Stream specifier. More than one Depot / Stream can be targeted by clicking "Add Stream".
5. Write the path to the Jenkinsfile.
NB. Lightweight checkout can be used to first try to checkout the Jenkinsfile for Pipeline configuration. If Lightweight checkout is not used, Jenkins will perform a full checkout to setup the pipeline.

### Use in Multibranch projects:

1. In Branch Sources -> Add source -> Accurev
2. Fill out the Host and Port of the Accurev server, and choose credentials. These will be used to access Accurev.
3. Define the depot to be accessed, if no top stream is chosen (see traits section), and all types is set to be discovered, the top stream will be the depot stream.
4. Choose types to discover. If none is checked, nothing will be found.
5. Traits can be added under the "Add" button. See traits section for individual traits explanation.
6. Under Build Configuration, specify path to Jenkinsfile
NB. if no Jenkinsfile is found in a stream, the stream is discarded as a possible build stream.

### Traits

**TopStream top discover from** - Specify a topstream as the upper limit, this makes it possible to only get a fraction of the streams in your depot.

**Types to discover** - Specify which types of items you want to discover. If none is selected, no items will be found.

**Exclude files** - To come

## DSL

### Accurev step
The syntax for Accurev checkout step is as follows:
```
accurev host: 'HOST', port: 'PORT', depot: 'DEPOT', credentialsId: 'CREDENTIALS'
```

### Triggers (Webhook, Polling)
To enable remote webhook triggers from Accurev, specify in the Jenkinsfile for the project:
```
triggers {
        accurevPush()
}
```



### Gated Streams
To use gated streams with the Accurev plugin, you have to put the triggers/server_master_trig.pl script under storage/site_slice/triggers and follow instructions inside on how to install. Furthermore the mqtt-gating-receiver.pl script needs to run on your accurev server, is this script is responsible for receiving MQTT messages and unlocking / promoting staged stream results.

### Respond to Accurev
In a Jenkinsfile, create a post { } section after the stages, and specify ```mqttResponse URL_TO_ACCUREV_SERVER```. That will send a respond back to the Mosquitto server that your Accurev server is using. 
