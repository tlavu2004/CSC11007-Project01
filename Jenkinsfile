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

        stage('Test') {
            steps {
                script {
                    def services = SERVICES.split()
                    for (service in services) {
                        echo "Testing: ${service}"
                        sh "./mvnw clean verify -pl ${service}"
                    }
                }
            }
            post {
                always {
                    script {
                        def changed = SERVICES.split()
                        def testPattern = changed.collect {
                            "${it}/target/surefire-reports/TEST-*.xml"
                        }.join(',')
                        def jacocoPattern = changed.collect {
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
                                classPattern: changed.collect { "${it}/target/classes" }.join(','),
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
                    def services = SERVICES.split()
                    services.each { service ->
                        def coverageReport = "${service}/target/site/jacoco/jacoco.xml"
                        def lineCoverage = sh(script: """
                            if [ -f ${coverageReport} ]; then
                                awk '
                                    /<counter type="LINE"/ {
                                        for (i = 1; i <= NF; i++) {
                                            if (\$i ~ /missed=/) {
                                                gsub(/[^0-9]/, "", \$i)
                                                missed += \$i
                                            } else if (\$i ~ /covered=/) {
                                                gsub(/[^0-9]/, "", \$i)
                                                covered += \$i
                                            }
                                        }
                                    }
                                    END {
                                        total = missed + covered
                                        if (total > 0) {
                                            coverage = (covered / total) * 100
                                            printf("%.4f", coverage)
                                        } else {
                                            print "0"
                                        }
                                    }
                                ' ${coverageReport}
                            else
                                echo "File not found: ${coverageReport}" > "/dev/stderr"
                                echo "0"
                            fi
                        """, returnStdout: true).trim()

                        if (lineCoverage) {
                            echo "Code coverage for ${service}: ${lineCoverage}%"
                        } else {
                            echo "No coverage report found for ${service}, assuming 0%"
                        }
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def services = SERVICES.split()
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