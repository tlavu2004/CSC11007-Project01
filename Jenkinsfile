pipeline {
    agent any

    options {
        buildDiscarder(logRotator(
            numToKeepStr: '10',
            artifactNumToKeepStr: '5'
        ))
    }

    stages {
        stage('Detect Changes') {
            steps {
                script {
                    sh 'pwd'

                    def changedFiles = sh(script: 'git diff --name-only HEAD~1 HEAD', returnStdout: true).trim()
                    echo "Changed files: ${changedFiles}"

                    def serviceMap = [
                        'spring-petclinic-genai-service': 'genai',
                        'spring-petclinic-customers-service': 'customers',
                        'spring-petclinic-vets-service': 'vets',
                        'spring-petclinic-visits-service': 'visits',
                        'spring-petclinic-api-gateway': 'api-gateway',
                        'spring-petclinic-discovery-server': 'discovery',
                        'spring-petclinic-config-server': 'config',
                        'spring-petclinic-admin-server': 'admin'
                    ]

                    def changedServices = serviceMap.findAll { entry -> changedFiles.contains(entry.key) }.collect { it.value }

                    if (changedServices.isEmpty()) {
                        changedServices = ['all']
                    }

                    echo "Detected changes in services: ${changedServices}"

                    CHANGED_SERVICES_LIST = changedServices
                    CHANGED_SERVICES_STRING = changedServices.join(',')
                    echo "Changed services: ${CHANGED_SERVICES_STRING}"
                }
            }
        }

        stage('Check Code Coverage') {
            steps {
                script {
                    def failedServices = []

                    CHANGED_SERVICES_LIST.each { service ->
                        if (service in ['customers', 'visits', 'vets']) {
                            def coverageReport = "spring-petclinic-${service}-service/target/site/jacoco/jacoco.xml"
                            def coverageThreshold = 70.0

                            def lineCoverage = sh(script: """
                                if [ -f ${coverageReport} ]; then
                                    awk '
                                        /<counter type="LINE"[^>]*missed=/ {
                                            split(\$0, a, "[ \\\"=]+");
                                            missed = a[2];
                                            covered = a[4];
                                            sum = missed + covered;
                                            coverage = (sum > 0 ? (covered / sum) * 100 : 0);
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
                    if (CHANGED_SERVICES_LIST.contains('all')) {
                        echo 'Building all modules'
                        sh './mvnw clean package -DskipTests'
                    } else {
                        def modules = CHANGED_SERVICES_LIST.join(',')
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
                    if (CHANGED_SERVICES_LIST.contains('all')) {
                        echo 'Testing all modules'
                        sh './mvnw clean test'
                    } else {
                        def modules = CHANGED_SERVICES_LIST.join(',')
                        echo "Testing modules: ${modules}"
                        sh "./mvnw clean test -pl ${modules}"
                    }
                }
            }
            post {
                always {
                    script {
                        def testReportPattern = ''
                        def jacocoPattern = ''

                        if (CHANGED_SERVICES_LIST.contains('all')) {
                            testReportPattern = '**/surefire-reports/TEST-*.xml'
                            jacocoPattern = '**/jacoco.exec'
                        } else {
                            def patterns = CHANGED_SERVICES_LIST.collect {
                                "spring-petclinic-${it}-service/target/surefire-reports/TEST-*.xml"
                            }.join(',')
                            testReportPattern = patterns

                            def jacocoPatterns = CHANGED_SERVICES_LIST.collect {
                                "spring-petclinic-${it}-service/target/jacoco.exec"
                            }.join(',')
                            jacocoPattern = jacocoPatterns
                        }

                        echo "Looking for test reports with pattern: ${testReportPattern}"
                        sh "find . -name 'TEST-*.xml' -type f"

                        def testFiles = sh(script: "find . -name 'TEST-*.xml' -type f", returnStdout: true).trim()
                        if (testFiles) {
                            echo "Found test reports: ${testFiles}"
                            junit testReportPattern
                        } else {
                            echo 'No test reports found, likely no tests were executed.'
                        }

                        echo "Looking for JaCoCo data with pattern: ${jacocoPattern}"
                        sh "find . -name 'jacoco.exec' -type f"

                        def jacocoFiles = sh(script: "find . -name 'jacoco.exec' -type f", returnStdout: true).trim()
                        if (jacocoFiles) {
                            echo "Found JaCoCo files: ${jacocoFiles}"
                            jacoco(
                                execPattern: jacocoPattern,
                                classPattern: CHANGED_SERVICES_LIST.contains('all') ? '**/target/classes' : CHANGED_SERVICES_LIST.collect { "spring-petclinic-${it}-service/target/classes" }.join(','),
                                sourcePattern: CHANGED_SERVICES_LIST.contains('all') ? '**/src/main/java' : CHANGED_SERVICES_LIST.collect { "spring-petclinic-${it}-service/src/main/java" }.join(',')
                            )
                        } else {
                            echo 'No JaCoCo execution data found, skipping coverage report.'
                        }
                    }
                }
            }
        }
    }
}
