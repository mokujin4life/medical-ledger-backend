def eurekaServer = new Structure(
        "eureka-server",
        "structural/eureka-server/build/Dockerfile")
def authService = new Structure(
        "auth-service",
        "structural/auth-service/build/Dockerfile")
def zuulGateway = new Structure(
        "zuul-gateway",
        "structural/zuul-gateway/build/Dockerfile")
def documentationService = new Structure(
        "documentation-service",
        "structural/documentation-service/build/Dockerfile")
def userService = new Structure(
        "user-service",
        "business/user-service/build/Dockerfile")
def selfSovereignIdentityService = new Structure(
        "self-sovereign-identity-service",
        "business/self-sovereign-identity-service/build/Dockerfile")
def governmentService = new Structure(
        "government-service",
        "business/fake-government-service/build/Dockerfile")

def modules = [
        eurekaServer,
        zuulGateway,
        authService,
        userService,
        selfSovereignIdentityService,
        governmentService,
        documentationService
]

pipeline {
    agent any

    stages {
        stage("maven build") {
            steps {
                println 'stage: maven build'
                withMaven(maven: 'maven3') {
                    sh 'mvn install:install-file -Dfile=./business/user-service/external/pixelmed.jar -DgroupId=pixelmed -DartifactId=pixelmed -Dversion=1.0 -Dpackaging=jar'
                    sh 'mvn -B -Dmaven.test.skip=true clean package'
                }
            }
        }

        stage('deploy') {
            steps {
                println 'stage: deploy'
                script {
                    def serviceName

                    def removeContainerCommand
                    def removeImageCommand
                    def buildCommand
                    def runCommand

                    String gitChanges = sh(
                            script: 'git diff --name-only HEAD^ HEAD',
                            returnStdout: true
                    ).trim()
                    println gitChanges

                    for (int i = 0; i < modules.size(); i++) {
                        serviceName = modules[i].serviceName
                        if (gitChanges.contains(serviceName)) {

                            int checkIfContainerExists = sh(
                                    script: 'docker ps -f name=' + serviceName + ' | grep -w ' + serviceName + ' | wc -l',
                                    returnStdout: true
                            ).trim()

                            if (checkIfContainerExists != 0) {
                                removeContainerCommand = 'docker rm \$(docker stop ' + serviceName + ')'
                                println removeContainerCommand
                                sh removeContainerCommand

                                removeImageCommand = 'docker rmi medical-ledger/' + serviceName
                                println removeImageCommand
                                sh removeImageCommand
                            }

                            buildCommand = 'docker build -t medical-ledger/' + serviceName + ' -f ' + modules[i].dockerPath + ' .'
                            println buildCommand
                            sh buildCommand

                            String volume = " -v /var/ledger:/var/ledger"
                            if (serviceName.equals("government-service"))
                                volume = " -v /var/government:/var/government"

                            runCommand = 'docker run -d --name ' + serviceName + volume + ' --net=host medical-ledger/' + serviceName
                            println runCommand
                            sh runCommand
                        }
                    }
                }
            }
        }
    }
}

class Structure {
    String serviceName
    String dockerPath

    Structure(String serviceName, String dockerPath) {
        this.dockerPath = dockerPath
        this.serviceName = serviceName
    }
}