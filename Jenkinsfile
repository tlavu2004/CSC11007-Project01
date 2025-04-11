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
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    }

    stages {
        stage("Checkout") {
            steps {
                checkout scm
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    sh "git fetch origin main:refs/remotes/origin/main"
                    def changes = sh(script: "git diff --name-only origin/main HEAD", returnStdout: true).trim().split("\n")
                    echo "Changed files: ${changes}"

                    def allServices = SERVICES.split().collect { it.trim() }

                    def changedServices = allServices.findAll { service ->
                        changes.any { it.contains(service) }
                    }

                    if (changedServices.isEmpty()) {
                        changedServices = ['all']
                    }

                    echo "Detected changed services: ${changedServices}"
                    env.CHANGED_SERVICES = changedServices.join(',')
                }
            }
        }

        stage('Check Code Coverage') {
            steps {
                script {
                    def coverageThreshold = 70.0
                    def failedServices = []
                    def changedList = env.CHANGED_SERVICES.split(',')

                    changedList.each { service ->
                        def coverageReport = "${service}/target/site/jacoco/jacoco.xml"

                        def lineCoverage = sh(script: """
                            if [ -f "${coverageReport}" ]; then
                                awk '
                                    /<counter type="LINE"[^>]*missed=/ {
                                        split(\\\$0, a, "[ \\\\\"=]+");
                                        missed = a[2];
                                        covered = a[4];
                                        sum = missed + covered;
                                        coverage = (sum > 0 ? (covered / sum) * 100 : 0);
                                        print coverage;
                                    }
                                ' "${coverageReport}"
                            else
                                echo "0"
                            fi
                        """, returnStdout: true).trim()

                        echo "Code coverage for ${service}: ${lineCoverage}%"
                        if (lineCoverage.toDouble() < coverageThreshold) {
                            failedServices.add(service)
                        }
                    }

                    if (!failedServices.isEmpty()) {
                        error "The following services failed code coverage threshold (${coverageThreshold}%): ${failedServices.join(', ')}"
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def modules = env.CHANGED_SERVICES
                    if (modules.contains('all')) {
                        echo "Building all modules"
                        sh './mvnw clean package -DskipTests'
                    } else {
                        echo "Building modules: ${modules}"
                        sh "./mvnw clean package -DskipTests -pl ${modules}"
                    }

                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    def modules = env.CHANGED_SERVICES
                    if (modules.contains('all')) {
                        echo "Testing all modules"
                        sh './mvnw clean test'
                    } else {
                        echo "Testing modules: ${modules}"
                        sh "./mvnw clean test -pl ${modules}"
                    }
                }
            }
            post {
                always {
                    script {
                        def changed = env.CHANGED_SERVICES.split(',')
                        def testPattern, jacocoPattern

                        if (changed.contains('all')) {
                            testPattern = '**/surefire-reports/TEST-*.xml'
                            jacocoPattern = '**/jacoco.exec'
                        } else {
                            testPattern = changed.collect {
                                "${it}/target/surefire-reports/TEST-*.xml"
                            }.join(',')
                            jacocoPattern = changed.collect {
                                "${it}/target/jacoco.exec"
                            }.join(',')
                        }

                        echo "Looking for test reports: ${testPattern}"
                        def testFiles = sh(script: "find . -name 'TEST-*.xml'", returnStdout: true).trim()
                        if (testFiles) {
                            junit testPattern
                        } else {
                            echo "No test reports found."
                        }

                        echo "Looking for jacoco files: ${jacocoPattern}"
                        def jacocoFiles = sh(script: "find . -name 'jacoco.exec'", returnStdout: true).trim()
                        if (jacocoFiles) {
                            jacoco(
                                execPattern: jacocoPattern,
                                classPattern: changed.contains('all') ? '**/target/classes' : changed.collect { "${it}/target/classes" }.join(','),
                                sourcePattern: changed.contains('all') ? '**/src/main/java' : changed.collect { "${it}/src/main/java" }.join(',')
                            )
                        } else {
                            echo "No jacoco files found."
                        }
                    }
                }
            }
        }
    }
}
