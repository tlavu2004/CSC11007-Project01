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
        CODECOV_TOKEN = credentials('codecov_token')
        COVERAGE_THRESHOLD = 70
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Changed Services') {
            steps {
                script {
                    // Fetch latest changes from main branch
                    sh 'git fetch origin main:refs/remotes/origin/main'

                    // Get the list of changed files between the current branch and main
                    def changedFiles = sh(
                        script: "git diff --name-only origin/main HEAD",
                        returnStdout: true
                    ).trim().split("\n")

                    echo "🔍 Changed files:\n${changedFiles.join('\n')}"

                    def changedServices = []
                    for (service in SERVICES.trim().split()) {
                        for (file in changedFiles) {
                            if (file.startsWith(service + "/")) {
                                changedServices << service
                                break
                            }
                        }
                    }

                    // If no changes are detected, skip the build
                    if (changedServices.isEmpty()) {
                        echo "✅ No service changes detected. Skipping build."
                        currentBuild.result = 'SUCCESS'
                        return
                    } else {
                        echo "📦 Changed services: ${changedServices}"
                        env.CHANGED_SERVICES = changedServices.join(',')
                    }
                }
            }
        }

        stage('Test & Coverage') {
            when {
                expression {
                    return env.CHANGED_SERVICES != null && env.CHANGED_SERVICES.trim()
                }
            }
            steps {
                script {
                    def services = env.CHANGED_SERVICES.split(',')
                    for (svc in services) {
                        echo "🧪 Testing: ${svc}"

                        // Run tests for the changed service
                        sh "./mvnw -pl ${svc} -am clean verify"

                        junit "**/${svc}/target/surefire-reports/*.xml"

                        // Parse coverage report (e.g., jacoco.xml)
                        def coverageFile = "${svc}/target/site/jacoco/jacoco.xml"
                        if (fileExists(coverageFile)) {
                            def coverageXml = readFile(coverageFile)

                            // Extract line coverage data
                            def lineCoveredMatches = (coverageXml =~ /counter type="LINE".*?covered="(\d+)"/)
                            def lineMissedMatches = (coverageXml =~ /counter type="LINE".*?missed="(\d+)"/)

                            if (lineCoveredMatches.count > 0 && lineMissedMatches.count > 0) {
                                def totalCovered = 0
                                def totalMissed = 0

                                // Process coverage data
                                for (int i = 0; i < lineCoveredMatches.count; i++) {
                                    totalCovered += lineCoveredMatches[i][1].toDouble()
                                }

                                for (int i = 0; i < lineMissedMatches.count; i++) {
                                    totalMissed += lineMissedMatches[i][1].toDouble()
                                }

                                // Calculate coverage percentage
                                def lineCoverage = (totalCovered / (totalCovered + totalMissed)) * 100

                                echo "📊 ${svc} Line Coverage: ${String.format('%.2f', lineCoverage)}%"

                                // Enforce coverage threshold
                                if (lineCoverage < COVERAGE_THRESHOLD.toDouble()) {
                                    error("Coverage check failed for ${svc}: ${String.format('%.2f', lineCoverage)}% < ${COVERAGE_THRESHOLD}%")
                                } else {
                                    echo "Coverage check passed for ${svc}"
                                }
                            } else {
                                error("Coverage data not found in ${coverageFile}")
                            }

                        } else {
                            echo "⚠️ Coverage file not found for ${svc}. Skipping coverage check."
                        }
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
                    for (svc in services) {
                        echo "Building (no tests): ${svc}"

                        // Build the service (without running tests)
                        sh "./mvnw -pl ${svc} -am package -DskipTests"
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished: ${currentBuild.result}"
            // Đảm bảo luôn thu thập báo cáo JaCoCo sau khi test
            junit '**/target/test-classes/*.xml'
            jacoco()
        }
        success {
            echo "All changed services built, tested, and coverage uploaded."
        }
        failure {
            echo "Build failed. Check logs and coverage for details."
        }
    }
}
