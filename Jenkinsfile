pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    options {
        buildDiscarder(
            logRotator(
                numToKeepStr: '10',
                artifactNumToKeepStr: '10'
            )
        )
        timeout(time: 30, unit: 'MINUTES')
        skipDefaultCheckout(false)
    }

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
        MAVEN_OPTS = '-Xmx1024m'
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
            when {
                expression {
                    return env.CHANGED_SERVICES != null && env.CHANGED_SERVICES.trim()
                }
            }
            steps {
                script {
                    // Helper function to get coverage from report
                    def getCoverageFromReport = { String reportPath ->
                        try {
                            echo "Checking coverage report at: ${reportPath}"
                            if (!fileExists(reportPath)) {
                                echo "ERROR: Coverage report file not found: ${reportPath}"
                                return 0.0
                            }

                            def coverage = sh(script: """
                                if command -v python3 > /dev/null; then
                                    echo "Using Python3 to parse coverage report..."
                                    python3 -c "
import xml.etree.ElementTree as ET
import sys
import os

report_path = '${reportPath}'
print('Processing file: ' + report_path)

if not os.path.exists(report_path):
    print('ERROR: File does not exist')
    print('0.00')
    sys.exit(0)

if not os.access(report_path, os.R_OK):
    print('ERROR: File is not readable')
    print('0.00')
    sys.exit(0)

try:
    tree = ET.parse(report_path)
    root = tree.getroot()
    print('Root element: ' + root.tag)
    
    total_missed = 0
    total_covered = 0
    counter_count = 0
    
    counters = root.findall('.//counter')
    for counter in counters:
        if counter.get('type') == 'LINE':
            counter_count += 1
            missed = counter.get('missed', '0')
            covered = counter.get('covered', '0')
            try:
                total_missed += int(missed)
                total_covered += int(covered)
            except ValueError as ve:
                print('ValueError: ' + str(ve))
                continue
    
    print('Total LINE counters: ' + str(counter_count))
    print('Missed: ' + str(total_missed) + ', Covered: ' + str(total_covered))
    
    total = total_missed + total_covered
    if total > 0:
        coverage_percent = (total_covered * 100.0) / total
        print('Coverage: ' + str(round(coverage_percent, 6)) + '%')
        print('COVERAGE_RESULT=' + str(round(coverage_percent, 6)))
    else:
        print('No coverage data')
        print('COVERAGE_RESULT=0.00')
        
except ET.ParseError as pe:
    print('Parse Error: ' + str(pe))
    print('0.00')
except Exception as e:
    print('Unexpected error: ' + str(e))
    print('0.00')
"
                                else
                                    echo "Using awk fallback"
                                    awk '
                                    BEGIN {
                                        missed = 0; covered = 0; counter_count = 0;
                                        print "Starting awk parsing..."
                                    }
                                    /<counter[^>]*type=\\"LINE\\"/ {
                                        counter_count++
                                        if (match(\$0, /missed=\\"([0-9]+)\\"/, arr)) {
                                            missed += arr[1]
                                        }
                                        if (match(\$0, /covered=\\"([0-9]+)\\"/, arr)) {
                                            covered += arr[1]
                                        }
                                    }
                                    END {
                                        total = missed + covered
                                        if (total > 0) {
                                            coverage_percent = (covered * 100.0) / total
                                            printf \\"COVERAGE_RESULT=%.6f\\n\\", coverage_percent
                                        } else {
                                            print \\"COVERAGE_RESULT=0.00\\"
                                        }
                                    }
                                    ' "${reportPath}"
                                fi
                            """, returnStdout: true)

                            echo "Raw coverage output:\n${coverage}"
                            def coverageResultLine = coverage.readLines().find { it.startsWith('COVERAGE_RESULT=') }
                            def coverageResult = coverageResultLine?.replace('COVERAGE_RESULT=', '')?.trim()
                            echo "Extracted: ${coverageResult}"
                            if (coverageResult?.isDouble()) {
                                return coverageResult.toDouble()
                            } else {
                                return 0.0
                            }
                        } catch (Exception e) {
                            echo "Error parsing report: ${e.message}"
                            return 0.0
                        }
                    }

                    def isDouble = { String str ->
                        try {
                            str.toDouble()
                            return true
                        } catch (Exception ignored) {
                            return false
                        }
                    }

                    // Phần chính: duyệt qua từng service
                    def failedServices = []
                    def changedServices = env.CHANGED_SERVICES.split(',')
                    def coverageThreshold = 70.0

                    changedServices.each { service ->
                        def coverageXmlPath = "${service}/target/site/jacoco/jacoco.xml"
                        def coverage = getCoverageFromReport(coverageXmlPath)

                        echo "Code coverage for ${service}: ${coverage}%"

                        if (coverage < coverageThreshold) {
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

    post {
        always {
            cleanWs(
                cleanWhenNotBuilt: false,
                deleteDirs: true,
                disableDeferredWipeout: true,
                notFailBuild: true
            )
        }
        success {
            echo "Pipeline completed successfully for services: ${env.CHANGED_SERVICES}"
        }
        failure {
            echo "Pipeline failed for services: ${env.CHANGED_SERVICES}"
        }
        unstable {
            echo "Pipeline completed with warnings for services: ${env.CHANGED_SERVICES}"
        }
    }
}