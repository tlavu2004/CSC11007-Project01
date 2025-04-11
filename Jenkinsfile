pipeline {
    agent any

    environment {
        MAVEN_OPTS = "-Dmaven.test.failure.ignore=false"
    }

    stages {
        stage('Checkout - Customer Service') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test - Customer Service') {
            steps {
                sh './mvnw clean verify'
            }
        }

        stage('Publish JaCoCo Report - Customer') {
            steps {
                jacoco(
                    execPattern: '**/target/jacoco.exec',
                    classPattern: '**/target/classes',
                    sourcePattern: '**/src/main/java'
                )
            }
        }

        stage('Check Code Coverage - Customer') {
            steps {
                script {
                    def coverage = jacoco()
                    def coverageRatio = coverage.instructionCoverage?.ratio ?: 0
                    echo "📊 Coverage (Customer): ${coverageRatio * 100}%"

                    if (coverageRatio < 0.70) {
                        error "❌ Build FAIL: Test coverage của customer-service thấp hơn 70%!"
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/site/jacoco/index.html', fingerprint: true
        }

        failure {
            echo '❌ Build customer-service thất bại: coverage chưa đạt yêu cầu hoặc test lỗi.'
        }

        success {
            echo '✅ Build customer-service thành công với coverage >= 70%'
        }
    }
}
