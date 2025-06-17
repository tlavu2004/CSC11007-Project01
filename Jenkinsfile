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
        COVERAGE_THRESHOLD = '70.0'
        MAVEN_OPTS = '-Xmx1024m'
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    try {
                        def changedFiles = sh(
                            script: 'git diff --name-only origin/main...HEAD || git ls-files', 
                            returnStdout: true
                        ).trim()
                        
                        if (!changedFiles) {
                            echo "No changes detected, treating as full build."
                            changedFiles = 'force-all'
                        } else {
                            echo "Changed files:\n${changedFiles}"
                        }

                        def serviceMap = [
                            'genai-service'     : 'spring-petclinic-genai-service',
                            'customers-service' : 'spring-petclinic-customers-service',
                            'vets-service'      : 'spring-petclinic-vets-service',
                            'visits-service'    : 'spring-petclinic-visits-service',
                            'api-gateway'       : 'spring-petclinic-api-gateway',
                            'discovery-server'  : 'spring-petclinic-discovery-server',
                            'config-server'     : 'spring-petclinic-config-server',
                            'admin-server'      : 'spring-petclinic-admin-server'
                        ]

                        def changedServices = []

                        // Check for service-specific changes
                        for (entry in serviceMap) {
                            if (changedFiles.contains(entry.value)) {
                                changedServices.add(entry.key)
                            }
                        }

                        // Check for root-level changes that affect all services
                        def rootFiles = ['pom.xml', 'docker-compose.yml', 'Jenkinsfile']
                        def hasRootChanges = rootFiles.any { changedFiles.contains(it) }

                        if (changedFiles == 'force-all' || hasRootChanges || changedServices.isEmpty()) {
                            echo "No specific service detected or root changes found. Will test and build all."
                            changedServices = ['all']
                        }

                        env.CHANGED_SERVICES = changedServices.join(',')
                        echo "Changed services: ${env.CHANGED_SERVICES}"
                        
                    } catch (Exception e) {
                        echo "Error detecting changes: ${e.message}. Defaulting to all services."
                        env.CHANGED_SERVICES = 'all'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')

                    try {
                        if (services.contains('all')) {
                            echo "Running tests for all modules..."
                            sh './mvnw clean verify'
                        } else {
                            def modules = services.collect { serviceMap[it] }.join(',')
                            echo "Running tests for: ${modules}"
                            sh "./mvnw clean verify -pl ${modules} -am"
                        }
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Tests failed: ${e.message}"
                        throw e
                    }
                }
            }

            post {
                always {
                    script {
                        def services = env.CHANGED_SERVICES.split(',')
                        def testPatterns = []

                        if (services.contains('all')) {
                            testPatterns << '**/target/surefire-reports/TEST-*.xml'
                        } else {
                            services.each { service ->
                                testPatterns << "spring-petclinic-${service}/target/surefire-reports/TEST-*.xml"
                            }
                        }

                        // Publish test results
                        try {
                            echo "Publishing test results: ${testPatterns.join(',')}"
                            junit testResults: testPatterns.join(','), allowEmptyResults: true
                        } catch (Exception e) {
                            echo "Failed to publish test results: ${e.message}"
                        }

                        // Publish JaCoCo coverage using Coverage Plugin per service
                        try {
                            echo "Publishing JaCoCo coverage reports per service"
                            if (services.contains('all')) {
                                recordCoverage tools: [jacoco()],
                                    skipPublishingChecks: true
                            } else {
                                services.each { service ->
                                    def reportPath = "spring-petclinic-${service}/target/site/jacoco/jacoco.xml"
                                    if (fileExists(reportPath)) {
                                        echo "Publishing coverage for ${service} using path: ${reportPath}"
                                        recordCoverage tools: [jacoco(pattern: reportPath)],
                                            skipPublishingChecks: true
                                    } else {
                                        echo "Coverage report not found for ${service}: ${reportPath}"
                                    }
                                }
                            }
                        } catch (Exception e) {
                            echo "Failed to publish coverage: ${e.message}"
                        }
                    }
                }
            }
        }

        stage('Check Code Coverage Threshold') {
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    def criticalServices = ['customers-service', 'visits-service', 'vets-service']
                    def failedServices = []

                    services.each { service ->
                        if (service == 'all' || criticalServices.contains(service)) {
                            def servicesToCheck = service == 'all' ? criticalServices : [service]
                            
                            servicesToCheck.each { svc ->
                                def reportPath = "${serviceMap[svc]}/target/site/jacoco/jacoco.xml"
                                
                                if (fileExists(reportPath)) {
                                    def coverage = getCoverageFromReport(reportPath)
                                    echo "Coverage for ${svc}-service: ${coverage}%"
                                    
                                    if (coverage < env.COVERAGE_THRESHOLD.toDouble()) {
                                        failedServices.add("${svc} (${coverage}%)")
                                    }
                                } else {
                                    echo "Coverage report not found for ${svc}: ${reportPath}"
                                    failedServices.add("${svc} (no report)")
                                }
                            }
                        }
                    }

                    if (!failedServices.isEmpty()) {
                        error "Coverage below threshold (${env.COVERAGE_THRESHOLD}%): ${failedServices.join(', ')}"
                    } else {
                        echo "All services meet coverage threshold of ${env.COVERAGE_THRESHOLD}%"
                    }
                }
            }
        }

        stage('Build') {
            when {
                expression { currentBuild.result == null || currentBuild.result != 'FAILURE' }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    
                    try {
                        if (services.contains('all')) {
                            echo "Building all modules..."
                            sh './mvnw clean package -DskipTests'
                        } else {
                            def modules = services.collect { 
                                "spring-petclinic-${it}" 
                            }.join(',')
                            echo "Building: ${modules}"
                            sh "./mvnw clean package -DskipTests -pl ${modules} -am"
                        }

                        // Archive artifacts
                        archiveArtifacts artifacts: '**/target/*.jar', 
                                       allowEmptyArchive: true,
                                       fingerprint: true,
                                       onlyIfSuccessful: true
                                       
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        echo "Build failed: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    echo "Quality gate passed for services: ${services.join(', ')}"
                }
            }
        }
    }

    post {
        always {
            cleanWs(cleanWhenNotBuilt: false,
                   deleteDirs: true,
                   disableDeferredWipeout: true,
                   notFailBuild: true)
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

// Helper function để parse JaCoCo coverage
def getCoverageFromReport(String reportPath) {
    try {
        // Using Python or awk to parse the JaCoCo XML report
        def coverage = sh(script: """
            if command -v python3 > /dev/null; then
                python3 -c "
import xml.etree.ElementTree as ET
try:
    tree = ET.parse('${reportPath}')
    root = tree.getroot()
    total_missed = 0
    total_covered = 0
    for counter in root.findall('.//counter[@type=\\\"LINE\\\"]'):
        total_missed += int(counter.get('missed', 0))
        total_covered += int(counter.get('covered', 0))
    total = total_missed + total_covered
    if total > 0:
        print(f'{(total_covered/total)*100:.2f}')
    else:
        print('0.00')
except Exception as e:
    print('0.00')
"
            else
                # Fallback: dùng awk
                awk '
                    /<counter type="LINE"/ {
                        for (i = 1; i <= NF; i++) {
                            if (\$i ~ /missed=/) { gsub(/[^0-9]/, "", \$i); missed += \$i }
                            if (\$i ~ /covered=/) { gsub(/[^0-9]/, "", \$i); covered += \$i }
                        }
                    }
                    END {
                        total = missed + covered;
                        if (total > 0) {
                            printf(\"%.2f\", (covered / total) * 100)
                        } else {
                            print \"0.00\"
                        }
                    }
                ' ${reportPath}
            fi
        """, returnStdout: true).trim()

        return coverage.toDouble()
    } catch (Exception e) {
        echo "Error parsing coverage report: ${e.message}"
        return 0.0
    }
}
