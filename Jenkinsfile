pipeline {
    agent any

    environment {
        MAVEN_OPTS = "-Dmaven.test.failure.ignore=false"
    }

    stages {
        stage('Checkout - Visits Service') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test - Visits Service') {
            steps {
                sh './mvnw clean verify'
            }
        }

        stage('Publish JaCoCo Report - Visits') {
            steps {
                jacoco(
                    execPattern: '**/target/jacoco.exec',
                    classPattern: '**/target/classes',
                    sourcePattern: '**/src/main/java'
                )
            }
        }

        stage('Check Code Coverage - Visits') {
            steps {
                script {
                    def coverage = jacoco()
                    def ratio = coverage.instructionCoverage?.ratio ?: 0
                    echo "📊 Coverage (Visits): ${ratio * 100}%"
                    if (ratio < 0.70) {
                        error "❌ Build FAIL: Test coverage của visits-service thấp hơn 70%!"
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
            echo '❌ Build visits-service thất bại: coverage chưa đạt yêu cầu hoặc test lỗi.'
        }

        success {
            echo '✅ Build visits-service thành công với coverage >= 70%'
        }
    }
}
