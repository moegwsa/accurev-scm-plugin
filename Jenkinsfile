pipeline {
    agent any
    tools {
    maven 'Standard'
    jdk 'JDK'
    }
    stages {
        stage('Build') {
            steps {
                bat 'mvn clean deploy'
            }
        }
    }
}