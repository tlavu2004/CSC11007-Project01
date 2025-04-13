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
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
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
                        echo "No service changes detected. Skipping build."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    echo "Detected changed services: ${changedServices}"
                    env.CHANGED_SERVICES = changedServices.join(',')
                }
            }
        }

        stage('Test') {
            when {
                expression {
                    return env.CHANGED_SERVICES != null && env.CHANGED_SERVICES.trim()
                }
            }
            
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (service in services) {
                        echo "Testing: ${service}"
                        sh "./mvnw clean verify -pl ${service}"
                    }   
                }
            }
            
            post {
                always {
                    script {
                        def changed = env.CHANGED_SERVICES.split(',')
                        def testPattern, jacocoPattern

                        testPattern = changed.collect {
                            "${it}/target/surefire-reports/TEST-*.xml"
                        }.join(',')
                        jacocoPattern = changed.collect {
                            "${it}/target/jacoco.exec"
                        }.join(',') 

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
                                classPattern:  changed.collect { "${it}/target/classes" }.join(','),
                                sourcePattern: changed.collect { "${it}/src/main/java" }.join(',')
                            )
                        } else {
                            echo "No jacoco files found."
                        }
                    }
                }
            }
        }

        stage('Check Code Coverage') {
            steps {
                script {
                    def failedServices = []
                    def changedServices = env.CHANGED_SERVICES.split(',')
                    def coverageThreshold = 70.0
                    changedServices.each { service ->
                        def coverageReport = "${service}/target/site/jacoco/jacoco.xml"
                        def lineCoverage = sh(script: """
                            if [ -f ${coverageReport} ]; then
                                awk '
                                    /<counter type="LINE"[^>]*missed=/ {
                                        split(\$0, a, "[ \\\"=]+");
                                        # Debug output
                                        print "Debug: Checking jacoco.xml for ${service}..." > "/dev/stderr";
                                        print "Raw line: " \$0 > "/dev/stderr";
                                        print "Array after split:" > "/dev/stderr";
                                        for (i in a) print "a[" i "] = " a[i] > "/dev/stderr";
                                        # Find missed and covered indices
                                        for (i in a) {
                                            if (a[i] == "missed") missed = a[i+1];
                                            if (a[i] == "covered") covered = a[i+1];
                                        }
                                        print "missed = " missed ", covered = " covered > "/dev/stderr";
                                        sum = missed + covered;
                                        print "sum (missed + covered) = " sum > "/dev/stderr";
                                        coverage = (sum > 0 ? (covered / sum) * 100 : 0);
                                        print "Coverage = " coverage "%" > "/dev/stderr";
                                        print "-----" > "/dev/stderr";
                                        # Output final coverage value to stdout
                                        print coverage;
                                    }
                                ' ${coverageReport}
                             else
                                echo "File not found: ${coverageReport}" > "/dev/stderr"
                                echo "0"
                            fi
                        """, returnStdout: true).trim()

                        if (lineCoverage) {
                            echo "Code coverage for ${service}: ${lineCoverage}%"
                            def coverageValue = lineCoverage.toDouble()
                            if (coverageValue < coverageThreshold) {
                                failedServices.add(service)
                            }
                        } else {
                            echo "No coverage report found for ${service}, assuming 0%"
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
            when {
                expression {
                    return env.CHANGED_SERVICES != null && env.CHANGED_SERVICES.trim()
                }
            }

            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (service in services) {
                        echo "Building: ${service}"
                        sh "./mvnw -pl ${service} -am package -DskipTests"
                    }  
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }
    }
}
