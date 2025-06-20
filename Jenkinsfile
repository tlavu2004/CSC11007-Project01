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
        COVERAGE_THRESHOLD = '30.0'
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

                        // Define service mapping as a global variable
                        env.SERVICE_MAP_GENAI = 'spring-petclinic-genai-service'
                        env.SERVICE_MAP_CUSTOMERS = 'spring-petclinic-customers-service'
                        env.SERVICE_MAP_VETS = 'spring-petclinic-vets-service'
                        env.SERVICE_MAP_VISITS = 'spring-petclinic-visits-service'
                        env.SERVICE_MAP_GATEWAY = 'spring-petclinic-api-gateway'
                        env.SERVICE_MAP_DISCOVERY = 'spring-petclinic-discovery-server'
                        env.SERVICE_MAP_CONFIG = 'spring-petclinic-config-server'
                        env.SERVICE_MAP_ADMIN = 'spring-petclinic-admin-server'

                        def serviceMap = [
                            'genai-service'     : env.SERVICE_MAP_GENAI,
                            'customers-service' : env.SERVICE_MAP_CUSTOMERS,
                            'vets-service'      : env.SERVICE_MAP_VETS,
                            'visits-service'    : env.SERVICE_MAP_VISITS,
                            'api-gateway'       : env.SERVICE_MAP_GATEWAY,
                            'discovery-server'  : env.SERVICE_MAP_DISCOVERY,
                            'config-server'     : env.SERVICE_MAP_CONFIG,
                            'admin-server'      : env.SERVICE_MAP_ADMIN
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
                        
                        // Check for services without test files (only for services that will be tested)
                        def servicesWithoutTests = []
                        def servicesToCheck = []
                        
                        if (changedServices.contains('all')) {
                            servicesToCheck = serviceMap.keySet() as List
                        } else {
                            servicesToCheck = changedServices
                        }
                        
                        servicesToCheck.each { service ->
                            def testDir = "${serviceMap[service]}/src/test/java"
                            if (!fileExists(testDir)) {
                                servicesWithoutTests.add(service)
                            } else {
                                // Check if there are actual test files
                                def testFiles = sh(
                                    script: "find ${testDir} -name '*Test.java' -o -name '*Tests.java' | wc -l",
                                    returnStdout: true
                                ).trim()
                                
                                if (testFiles == '0') {
                                    servicesWithoutTests.add(service)
                                }
                            }
                        }
                        
                        env.SERVICES_WITHOUT_TESTS = servicesWithoutTests.join(',')
                        if (!servicesWithoutTests.isEmpty()) {
                            echo "Services without test files: ${servicesWithoutTests.join(', ')}"
                        }
                        
                    } catch (Exception e) {
                        echo "Error detecting changes: ${e.message}. Defaulting to all services."
                        env.CHANGED_SERVICES = 'all'
                        env.SERVICES_WITHOUT_TESTS = ''
                    }
                }
            }
        }

        stage('Test') {
            environment {
                SERVICES = '' // Using environment variables to map services
            }
            steps {
                script {
                    // Recreate service map
                    def serviceMap = [
                        'genai-service'     : env.SERVICE_MAP_GENAI,
                        'customers-service' : env.SERVICE_MAP_CUSTOMERS,
                        'vets-service'      : env.SERVICE_MAP_VETS,
                        'visits-service'    : env.SERVICE_MAP_VISITS,
                        'api-gateway'       : env.SERVICE_MAP_GATEWAY,
                        'discovery-server'  : env.SERVICE_MAP_DISCOVERY,
                        'config-server'     : env.SERVICE_MAP_CONFIG,
                        'admin-server'      : env.SERVICE_MAP_ADMIN
                    ]

                    def services = (env.CHANGED_SERVICES ?: '').split(',')
                    env.SERVICES = services.join(',') // Lưu vào biến môi trường để post block dùng được

                    try {
                        if (services.contains('all')) {
                            echo "Running tests for all modules..."
                            sh "./mvnw clean verify -DskipTests=false -Djacoco.skip=false"
                            sh "find . -name 'jacoco.xml' || true"
                        } else {
                            def modules = services.collect { serviceMap[it] }.join(',')
                            echo "Running tests for: ${modules}"
                            sh "./mvnw clean verify -pl ${modules} -am -DskipTests=false -Djacoco.skip=false"
                            sh "ls -l ${modules.replace(',', ' ')}/target/site/jacoco/jacoco.xml || true"
                            sh "find . -name 'jacoco.xml' || true"
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
                        echo "Publishing JaCoCo coverage reports"

                        def services = (env.SERVICES ?: '').split(',')

                        def serviceMap = [
                            'genai-service'     : env.SERVICE_MAP_GENAI,
                            'customers-service' : env.SERVICE_MAP_CUSTOMERS,
                            'vets-service'      : env.SERVICE_MAP_VETS,
                            'visits-service'    : env.SERVICE_MAP_VISITS,
                            'api-gateway'       : env.SERVICE_MAP_GATEWAY,
                            'discovery-server'  : env.SERVICE_MAP_DISCOVERY,
                            'config-server'     : env.SERVICE_MAP_CONFIG,
                            'admin-server'      : env.SERVICE_MAP_ADMIN
                        ]

                        def patterns = []
                        if (services.contains('all')) {
                            patterns = ['**/target/site/jacoco/jacoco.xml']
                        } else {
                            for (svc in services) {
                                def p = "${serviceMap[svc]}/target/site/jacoco/jacoco.xml"
                                if (fileExists(p)) {
                                    patterns << p
                                }
                            }
                        }

                        patterns.each { echo "Found coverage report: ${it}" }

                        if (patterns) {
                            recordCoverage(
                                tools: [[
                                    parser: 'JACOCO',
                                    path: patterns.join(',')
                                ]],
                                enabledForFailure: true,
                                skipPublishingChecks: true
                            )
                        } else {
                            echo "No coverage reports found"
                        }
                    }
                }
            }
        }

        stage('Check Code Coverage Threshold') {
            steps {
                script {
                    // Recreate service map
                    def serviceMap = [
                        'genai-service'     : env.SERVICE_MAP_GENAI,
                        'customers-service' : env.SERVICE_MAP_CUSTOMERS,
                        'vets-service'      : env.SERVICE_MAP_VETS,
                        'visits-service'    : env.SERVICE_MAP_VISITS,
                        'api-gateway'       : env.SERVICE_MAP_GATEWAY,
                        'discovery-server'  : env.SERVICE_MAP_DISCOVERY,
                        'config-server'     : env.SERVICE_MAP_CONFIG,
                        'admin-server'      : env.SERVICE_MAP_ADMIN
                    ]
                    
                    def services = (env.CHANGED_SERVICES ?: '').split(',')
                    def servicesWithoutTests = (env.SERVICES_WITHOUT_TESTS ?: '').split(',').findAll { it.trim() }
                    
                    // Define service categories
                    def criticalServices = ['customers-service', 'vets-service', 'visits-service']
                    def servicesWithTests = ['api-gateway', 'config-server', 'discovery-server']
                    def servicesWithoutTestFiles = ['admin-server', 'genai-service']
                    
                    def servicesToCheck = []
                    def failedServices = []
                    
                    // Logic to determine which services to check for coverage:
                    // 1. Critical services: ALWAYS check code coverage
                    // 2. Other services: Just check if they have changed AND have test files
                    
                    echo "=== COVERAGE CHECK LOGIC ==="
                    
                    // 1. ALWAYS check critical services (customers, vets, visits)
                    criticalServices.each { svc ->
                        servicesToCheck.add(svc)
                        echo "Adding ${svc} to coverage check (CRITICAL service - always check)"
                    }

                    // 2. Only check other services if they have changed AND have test files
                    def otherChangedServices = []
                    if (services.contains('all')) {
                        otherChangedServices = servicesWithTests + servicesWithoutTestFiles
                        echo "All services changed, checking non-critical services"
                    } else {
                        otherChangedServices = services.findAll { !criticalServices.contains(it) }
                        echo "Non-critical services changed: ${otherChangedServices.join(', ')}"
                    }
                    
                    otherChangedServices.each { svc ->
                        if (servicesWithTests.contains(svc)) {
                            servicesToCheck.add(svc)
                            echo "Adding ${svc} to coverage check (has tests + changed)"
                        } else if (servicesWithoutTestFiles.contains(svc)) {
                            echo "Skipping ${svc} coverage check (no test files)"
                        } else {
                            echo "Unknown service: ${svc}"
                        }
                    }
                    
                    echo "Final services to check coverage: ${servicesToCheck.join(', ')}"
                    
                    if (servicesToCheck.isEmpty()) {
                        echo "No services require coverage check."
                        return
                    }
                    
                    // Check coverage for each service
                    servicesToCheck.each { svc ->
                        def reportPath = "${serviceMap[svc]}/target/site/jacoco/jacoco.xml"
                        
                        if (fileExists(reportPath)) {
                            def coverage = getCoverageFromReport(reportPath)
                            echo "Coverage for ${svc}: ${coverage}%"
                            
                            if (coverage < env.COVERAGE_THRESHOLD.toDouble()) {
                                def reason = criticalServices.contains(svc) ? "CRITICAL" : "CHANGED+HAS_TESTS"
                                failedServices.add("${svc} (${coverage}%) [${reason}]")
                            }
                        } else {
                            echo "Coverage report not found for ${svc}: ${reportPath}"
                            def reason = criticalServices.contains(svc) ? "CRITICAL" : "CHANGED+HAS_TESTS"
                            failedServices.add("${svc} (no report) [${reason}]")
                        }
                    }

                    if (!failedServices.isEmpty()) {
                        error """
PIPELINE FAILED: Services below coverage threshold (${env.COVERAGE_THRESHOLD}%):
${failedServices.collect { "   • ${it}" }.join('\n')}

Coverage Rules:
• CRITICAL services (customers, vets, visits): Always checked
• OTHER services: Only checked when changed AND have test files
• Services without tests: Skipped

Action required: Improve test coverage before proceeding
"""
                    } else {
                        echo "All required services meet coverage threshold of ${env.COVERAGE_THRESHOLD}%"
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
                    // Recreate service map
                    def serviceMap = [
                        'genai-service'     : env.SERVICE_MAP_GENAI,
                        'customers-service' : env.SERVICE_MAP_CUSTOMERS,
                        'vets-service'      : env.SERVICE_MAP_VETS,
                        'visits-service'    : env.SERVICE_MAP_VISITS,
                        'api-gateway'       : env.SERVICE_MAP_GATEWAY,
                        'discovery-server'  : env.SERVICE_MAP_DISCOVERY,
                        'config-server'     : env.SERVICE_MAP_CONFIG,
                        'admin-server'      : env.SERVICE_MAP_ADMIN
                    ]
                    
                    def services = (env.CHANGED_SERVICES ?: '').split(',')
                    
                    try {
                        if (services.contains('all')) {
                            echo "Building all modules..."
                            sh './mvnw clean package -DskipTests'
                        } else {
                            def modules = services.collect { 
                                serviceMap[it] 
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
                    def services = (env.CHANGED_SERVICES ?: '').split(',')
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
                # Fallback: Using awk
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
