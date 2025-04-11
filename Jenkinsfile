pipeline {
    agent any

    environment {
        MAVEN_OPTS = "-Dmaven.test.failure.ignore=false"
    }

    stages {
        stage('Checkout - Vets Service') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test - Vets Service') {
            steps {
                sh './mvnw clean verify'
            }
        }

        stage('Publish JaCoCo Report - Vets') {
            steps {
                jacoco(
                    execPattern: '**/target/jacoco.exec',
                    classPattern: '**/target/classes',
                    sourcePattern: '**/src/main/java'
                )
            }
        }

        stage('Check Code Coverage - Vets') {
            steps {
                script {
                    def coverage = jacoco()
                    def ratio = coverage.instructionCoverage?.ratio ?: 0
                    echo "📊 Coverage (Vets): ${ratio * 100}%"
                    if (ratio < 0.70) {
                        error "❌ Build FAIL: Test coverage của vets-service thấp hơn 70%!"
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
            echo '❌ Build vets-service thất bại: coverage chưa đạt yêu cầu hoặc test lỗi.'
        }

        success {
            echo '✅ Build vets-service thành công với coverage >= 70%'
        }
    }
}
