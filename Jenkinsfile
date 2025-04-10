pipeline {
    agent any
    environment {
        SERVICES = """
            spring-petclinic-admin-server
            spring-petclinic-api-gateway
            spring-petclinic-config-server
            spring-petclinic-customers-service
            spring-petclinic-discovery-server
            spring-petclinic-genai-service
            spring-petclinic-vets-service
            spring-petclinic-visits-service
        """
    stages {
        stage ("Checkout") {
            steps {
                script {
                    checkout scm
                }
            }
        }
        stage ("Detect changes") {
            steps {
                script {
                    sh "git fetch origin main:refs/remotes/origin/main"
                    def changes = sh (script: "git diff --name-only origin/main HEAD", returnStdout: true).trim().split("\n")
                    echo "Changes file: ${changes}"
                    def changedServices = []
                    for (service in SERVICES.split()) {
                        if (changes.any { it.contains(service) }) {
                            changedServices.add(service)
                        }
                    }
                    if (changedServices.isEmpty()) {
                        echo " No service changes detected"
                        currentBuild.result = 'SUCCESS'
                        return
                    } else {
                        echo "Changed services: ${changedServices}"
                        env.CHANGED_SERVICES = changedServices.join(',')
                    }
   
                }

            }
        }
        stage('Build') {
            when {
                expression {
                    return env.CHANGED_SERVICES != null && env.CHANGED_SERVICES.trim()
                }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (service in services) {
                        echo " Building: ${service}"
                        sh "./mvnw -pl ${service} -am package -DskipTests"
                    }
                }
            }
        }
    }
}