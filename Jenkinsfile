pipeline {
    agent any

    triggers {
        githubPush()  // Nhận sự kiện push từ GitHub
    }

    environment {
        CURRENT_BRANCH = "${env.BRANCH_NAME}"
        CHANGED_SERVICES = "customer-service,vets-service,visits-service"
        MAVEN_OPTS = "-Dmaven.test.failure.ignore=false"
    }

    stages {
        stage('Info') {
            steps {
                echo "🔎 Running pipeline on branch: ${env.BRANCH_NAME}"
            }
        }

        stage('Build') {
            steps {
                echo "🔨 Building project on ${env.BRANCH_NAME}"
                // Thêm build thật nếu cần
                // sh './mvnw clean install'
            }
        }

        stage('Test & Coverage per Service') {
            steps {
                script {
                    def services = CHANGED_SERVICES.split(',')
                    for (svc in services) {
                        echo "🚀 Testing service: ${svc}"

                        // Chạy test và sinh coverage
                        sh "./mvnw -pl ${svc} -am clean verify"

                        // Publish unit test result
                        junit "**/${svc}/target/surefire-reports/*.xml"

                        // Đọc file coverage XML
                        def coverageFile = "${svc}/target/site/jacoco/jacoco.xml"
                        def coverageXml = readFile(coverageFile)

                        // Trích xuất và tính coverage %
                        def matcher = coverageXml =~ /<counter type="INSTRUCTION" missed="(\\d+)" covered="(\\d+)"\\/>/
                        if (matcher.find()) {
                            def missed = matcher[0][1].toInteger()
                            def covered = matcher[0][2].toInteger()
                            def total = missed + covered
                            def ratio = covered / total

                            echo "📊 Coverage của ${svc}: ${(ratio * 100).round(2)}%"

                            if (ratio < 0.70) {
                                error "⛔ Build FAIL: Coverage của ${svc} thấp hơn 70%!"
                            }
                        } else {
                            error "Không tìm thấy dữ liệu coverage trong ${coverageFile}"
                        }

                        // Lưu báo cáo coverage HTML
                        archiveArtifacts artifacts: "${svc}/target/site/jacoco/index.html", fingerprint: true
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo "🚀 Deploying production build from ${env.BRANCH_NAME}"
                // sh './deploy.sh'
            }
        }
    }

    post {
        success {
            echo "✅ Build & test succeeded on branch: ${env.BRANCH_NAME}"
        }
        failure {
            echo "❌ Build or test failed on branch: ${env.BRANCH_NAME}"
        }
    }
}
