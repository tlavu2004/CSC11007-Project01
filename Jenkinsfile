pipeline {
    agent any

    triggers {
        githubPush()  // Nhận sự kiện push từ GitHub
    }

    environment {
        CURRENT_BRANCH = "${env.BRANCH_NAME}"
    }

    stages {
        stage('Info') {
            steps {
                echo "Running pipeline on branch: ${env.BRANCH_NAME}"
                script {
                    githubChecks(
                        name: 'CI',
                        status: 'PENDING',
                        description: 'Pipeline started'
                    )
                }
            }
        }

        stage('Build') {
            steps {
                echo "Building project on ${env.BRANCH_NAME}"
                // Thêm build thật nếu cần
                // sh './mvnw clean install'
            }
        }

        stage('Test') {
            steps {
                echo "Running tests on ${env.BRANCH_NAME}"
                // sh './mvnw test'
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo "Deploying production build!"
                // sh './deploy.sh'
            }
        }
    }

    post {
        success {
            echo "✅ Build & test succeeded on branch: ${env.BRANCH_NAME}"
            script {
                githubChecks(
                    name: 'CI',
                    status: 'SUCCESS',
                    description: 'All checks passed'
                )
            }
        }
        failure {
            echo "❌ Build or test failed on branch: ${env.BRANCH_NAME}"
            script {
                githubChecks(
                    name: 'CI',
                    status: 'FAILURE',
                    description: 'Checks failed'
                )
            }
        }
    }
}
