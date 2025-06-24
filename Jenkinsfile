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
                        def changedFiles = ''
                        def gitCommand = ''
                        
                        // Determine git command based on build trigger
                        if (env.CHANGE_ID) {
                            // This is a PR build
                            gitCommand = "git diff --name-only origin/main...HEAD"
                            echo "PR build detected - comparing against main branch"
                        } else {
                            // This is a main branch build (after merge)
                            gitCommand = "git diff --name-only HEAD~1..HEAD"
                            echo "Main branch build detected - comparing with previous commit"
                        }
                        
                        try {
                            changedFiles = sh(
                                script: gitCommand,
                                returnStdout: true
                            ).trim()
                        } catch (Exception gitError) {
                            echo "Primary git diff failed, trying fallback..."
                            try {
                                changedFiles = sh(
                                    script: 'git diff --name-only HEAD~1',
                                    returnStdout: true
                                ).trim()
                            } catch (Exception gitError2) {
                                echo "All git diff commands failed"
                                changedFiles = ''
                            }
                        }
                        
                        if (!changedFiles) {
                            echo "No changes detected. This might be:"
                            echo "  - First commit in repo"
                            echo "  - Manual build"
                            echo "  - Git diff command issue"
                            
                            // Check build cause
                            def buildCause = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
                            def isManualBuild = !buildCause.isEmpty()
                            
                            if (isManualBuild) {
                                echo "Manual build detected - building all services"
                                changedFiles = 'MANUAL_BUILD_ALL'
                            } else {
                                // For automated builds with no changes, still build if this is main branch
                                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                                    echo "Main branch build with no detected changes - building all services"
                                    changedFiles = 'MAIN_BRANCH_BUILD'
                                } else {
                                    echo "No changes and not main branch - skipping pipeline"
                                    currentBuild.result = 'NOT_BUILT'
                                    currentBuild.description = 'No changes detected - pipeline skipped'
                                    return
                                }
                            }
                        } else {
                            echo "Changed files detected:"
                            changedFiles.split('\n').each { file ->
                                echo "  - ${file}"
                            }
                        }

                        // Define service mapping
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

                        // Service detection logic
                        if (changedFiles == 'MANUAL_BUILD_ALL' || changedFiles == 'MAIN_BRANCH_BUILD') {
                            changedServices = ['all']
                        } else {
                            // Check for root-level changes first
                            def rootFiles = ['pom.xml', 'docker-compose.yml', 'Jenkinsfile']
                            def hasRootChanges = rootFiles.any { rootFile ->
                                changedFiles.split('\n').any { it.trim() == rootFile }
                            }
                            
                            if (hasRootChanges) {
                                echo "Root level changes detected - will build all services"
                                changedServices = ['all']
                            } else {
                                // Check for service-specific changes
                                def changedFilesList = changedFiles.split('\n').collect { it.trim() }
                                
                                serviceMap.each { serviceName, servicePath ->
                                    def hasServiceChanges = changedFilesList.any { file ->
                                        file.startsWith("${servicePath}/")
                                    }
                                    
                                    if (hasServiceChanges) {
                                        changedServices.add(serviceName)
                                        echo "Service ${serviceName} has changes in directory: ${servicePath}/"
                                    }
                                }
                            }
                        }

                        if (changedServices.isEmpty()) {
                            echo "No service-specific changes detected. Skipping pipeline."
                            currentBuild.result = 'NOT_BUILT'
                            currentBuild.description = 'No service changes detected - pipeline skipped'
                            return
                        }

                        env.CHANGED_SERVICES = changedServices.join(',')
                        echo "Final changed services: ${env.CHANGED_SERVICES}"
                        
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

                    def services = (env.CHANGED_SERVICES ?: '').split(',') as List
                    def servicesToTest = []

                    echo "=== TESTING PHASE - Only services with test files ==="

                    // Step 2: Test only changed services that have test files
                    if (services.contains('all')) {
                        echo "All services changed, checking all services for test files..."
                        serviceMap.each { svcName, svcPath ->
                            def testDir = "${svcPath}/src/test/java"
                            if (fileExists(testDir)) {
                                def testFiles = sh(
                                    script: "find ${testDir} -name '*Test.java' -o -name '*Tests.java' | wc -l",
                                    returnStdout: true
                                ).trim()
                                
                                if (testFiles != '0') {
                                    servicesToTest.add(svcName)
                                    echo "Service ${svcName} has ${testFiles} test files - will be tested"
                                } else {
                                    echo "Service ${svcName} has no test files - skipping"
                                }
                            } else {
                                echo "Service ${svcName} has no test directory - skipping"
                            }
                        }
                    } else {
                        echo "Checking changed services for test files: ${services.join(', ')}"
                        services.each { svcName ->
                            def svcPath = serviceMap[svcName]
                            def testDir = "${svcPath}/src/test/java"
                            if (fileExists(testDir)) {
                                def testFiles = sh(
                                    script: "find ${testDir} -name '*Test.java' -o -name '*Tests.java' | wc -l",
                                    returnStdout: true
                                ).trim()
                                
                                if (testFiles != '0') {
                                    servicesToTest.add(svcName)
                                    echo "Service ${svcName} has ${testFiles} test files - will be tested"
                                } else {
                                    echo "Service ${svcName} has no test files - skipping"
                                }
                            } else {
                                echo "Service ${svcName} has no test directory - skipping"
                            }
                        }
                    }

                    // Store services that will be tested for coverage check
                    env.SERVICES_TO_TEST = servicesToTest.join(',')

                    if (servicesToTest.isEmpty()) {
                        echo "No services with test files to test - skipping test phase"
                        return
                    }

                    try {
                        def modules = servicesToTest.collect { serviceMap[it] }.join(',')
                        echo "Running tests for services with test files: ${modules}"
                        sh "./mvnw clean verify -pl ${modules} -am -DskipTests=false -Djacoco.skip=false"
                        
                        // Debug: Check generated jacoco files
                        servicesToTest.each { svc ->
                            def jacocoPath = "${serviceMap[svc]}/target/site/jacoco/jacoco.xml"
                            echo "Checking jacoco file for ${svc}: ${jacocoPath}"
                            sh "ls -l ${jacocoPath} || echo 'File not found'"
                        }
                        sh "find . -name 'jacoco.xml' || true"
                        
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

                        def servicesToTest = (env.SERVICES_TO_TEST ?: '').split(',') as List
                        if (servicesToTest.isEmpty() || servicesToTest[0] == '') {
                            echo "No services were tested - skipping coverage report"
                            return
                        }

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
                        for (svc in servicesToTest) {
                            def p = "${serviceMap[svc]}/target/site/jacoco/jacoco.xml"
                            echo "Checking fileExists for: ${p}"
                            if (fileExists(p)) {
                                patterns << p
                                echo "Found coverage file: ${p}"
                            } else {
                                echo "Coverage file not found: ${p}"
                                sh "ls -lah ${serviceMap[svc]}/target/site/jacoco || true"
                            }
                        }

                        if (patterns) {
                            echo "Using coverage files: ${patterns.join(',')}"

                            // Collect source directories for all tested services
                            def sourceDirectories = []
                            for (svc in servicesToTest) {
                                def sourceDir = "${serviceMap[svc]}/src/main/java"
                                if (fileExists(sourceDir)) {
                                    sourceDirectories << [path: sourceDir]
                                    echo "Added source directory: ${sourceDir}"
                                }
                            }

                            // Debug source directories
                            servicesToTest.each { svc ->
                                def sourceDir = "${serviceMap[svc]}/src/main/java"
                                echo "Checking source directory for ${svc}: ${sourceDir}"
                                sh "ls -la ${sourceDir} || echo 'Directory not found'"
                            }

                            recordCoverage(
                                tools: [[
                                    parser: 'JACOCO',
                                    path: patterns.join(',')
                                ]],
                                sourceDirectories: sourceDirectories,
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
                    def servicesToTest = (env.SERVICES_TO_TEST ?: '').split(',') as List
                    if (servicesToTest.isEmpty() || servicesToTest[0] == '') {
                        echo "No services were tested - skipping coverage check"
                        return
                    }

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
                    
                    def failedServices = []
                    
                    echo "=== COVERAGE CHECK - Services that were tested (${env.COVERAGE_THRESHOLD}% threshold) ==="
                    
                    // Step 3: Check coverage only for services that were tested
                    servicesToTest.each { svc ->
                        def reportPath = "${serviceMap[svc]}/target/site/jacoco/jacoco.xml"
                        
                        if (fileExists(reportPath)) {
                            def coverage = getCoverageFromReport(reportPath)
                            echo "Coverage for ${svc}: ${coverage}%"
                            
                            if (coverage < env.COVERAGE_THRESHOLD.toDouble()) {
                                failedServices.add("${svc} (${coverage}%)")
                            }
                        } else {
                            echo "ERROR: No coverage report found for ${svc} - this should not happen"
                            failedServices.add("${svc} (no report)")
                        }
                    }

                    if (!failedServices.isEmpty()) {
                        error """
PIPELINE FAILED: Services below coverage threshold (${env.COVERAGE_THRESHOLD}%):
${failedServices.collect { "   • ${it}" }.join('\n')}

Coverage check failed - pipeline stopped.
"""
                    } else {
                        echo "All tested services meet coverage threshold of ${env.COVERAGE_THRESHOLD}%"
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
                    
                    def services = (env.CHANGED_SERVICES ?: '').split(',') as List
                    
                    echo "=== BUILD PHASE - All changed services (including those without tests) ==="
                    
                    try {
                        // Step 4: Build all changed services (including those without tests)
                        if (services.contains('all')) {
                            echo "Building all modules..."
                            sh './mvnw clean package -DskipTests'
                        } else {
                            def modules = services.collect { 
                                serviceMap[it] 
                            }.join(',')
                            echo "Building changed services: ${modules}"
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
                    def services = (env.CHANGED_SERVICES ?: '').split(',') as List
                    echo "Quality gate passed for services: ${services.join(', ')}"
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

// Helper function để parse JaCoCo coverage
def getCoverageFromReport(String reportPath) {
    try {
        // Log the file path being processed
        echo "Checking coverage report at: ${reportPath}"
        
        // Check if the report file exists
        if (!fileExists(reportPath)) {
            echo "ERROR: Coverage report file not found: ${reportPath}"
            return 0.0
        }

        // Parse JaCoCo XML report using Python or awk fallback
        def coverage = sh(script: """
            if command -v python3 > /dev/null; then
                echo "Using Python3 to parse coverage report..."
                python3 -c "
import xml.etree.ElementTree as ET
import sys
import os

report_path = '${reportPath}'
print('Processing file: ' + report_path)

# Check if file exists and is readable
if not os.path.exists(report_path):
    print('ERROR: File does not exist')
    print('0.00')
    sys.exit(0)

if not os.access(report_path, os.R_OK):
    print('ERROR: File is not readable')
    print('0.00')
    sys.exit(0)

try:
    # Parse XML document
    tree = ET.parse(report_path)
    root = tree.getroot()
    print('Root element: ' + root.tag)
    
    total_missed = 0
    total_covered = 0
    counter_count = 0
    
    # Find all counter elements with type='LINE'
    # Fix: Use proper XPath syntax
    counters = root.findall('.//counter')
    for counter in counters:
        if counter.get('type') == 'LINE':
            counter_count += 1
            missed = counter.get('missed', '0')
            covered = counter.get('covered', '0')
            print('Counter ' + str(counter_count) + ': missed=' + missed + ', covered=' + covered)
            
            # Ensure values are numeric
            try:
                total_missed += int(missed)
                total_covered += int(covered)
            except ValueError as ve:
                print('ValueError converting values: ' + str(ve))
                continue
    
    print('Total LINE counters found: ' + str(counter_count))
    print('Total missed: ' + str(total_missed) + ', Total covered: ' + str(total_covered))
    
    total = total_missed + total_covered
    if total > 0:
        coverage_percent = (total_covered * 100.0) / total
        print('Coverage: ' + str(round(coverage_percent, 6)) + '%')
        print(str(round(coverage_percent, 6)))
    else:
        print('No coverage data found')
        print('0.00')
        
except ET.ParseError as pe:
    print('XML Parse Error: ' + str(pe))
    print('0.00')
except Exception as e:
    print('Unexpected error: ' + str(e))
    print('0.00')
"
            else
                echo "Using awk to parse coverage report..."
                # Fallback: Use awk for parsing
                awk '
                BEGIN { 
                    missed = 0; 
                    covered = 0; 
                    counter_count = 0;
                    print "Starting awk parsing..."
                }
                # Look for counter elements with type="LINE"
                /<counter[^>]*type="LINE"/ {
                    counter_count++
                    print "Processing line:", \$0
                    
                    # Extract missed attribute value
                    if (match(\$0, /missed="([0-9]+)"/, arr)) {
                        missed += arr[1]
                        print "Found missed:", arr[1]
                    }
                    # Extract covered attribute value
                    if (match(\$0, /covered="([0-9]+)"/, arr)) {
                        covered += arr[1]
                        print "Found covered:", arr[1]
                    }
                }
                END {
                    print "Total LINE counters:", counter_count
                    print "Total missed:", missed, "Total covered:", covered
                    
                    total = missed + covered;
                    if (total > 0) {
                        coverage_percent = (covered * 100.0) / total
                        printf "Coverage: %.6f%%\\n", coverage_percent
                        printf "%.6f", coverage_percent
                    } else {
                        print "No coverage data found"
                        print "0.00"
                    }
                }
                ' "${reportPath}"
            fi
        """, returnStdout: true)

        echo "Raw coverage output:\\n${coverage}"
        
        // Extract the last line (coverage result)
        def lines = coverage.split('\\n')
        def coverageResult = lines[-1].trim()
        
        echo "Extracted coverage result: '${coverageResult}'"

        // Validate and convert result
        if (coverageResult?.isDouble()) {
            def result = coverageResult.toDouble()
            echo "Final code coverage: ${result}%"
            return result
        } else {
            echo "Invalid coverage result format: '${coverageResult}'"
            return 0.0
        }
        
    } catch (Exception e) {
        echo "Error parsing coverage report: ${e.message}"
        echo "Exception details: ${e.toString()}"
        return 0.0
    }
}

// Helper: check if string can be converted to double
boolean isDouble(String str) {
    try {
        str.toDouble()
        return true
    } catch (Exception ignored) {
        return false
    }
}
