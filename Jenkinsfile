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
            }
        }

        stage('Build') {
            steps {
                echo "Building project on ${env.BRANCH_NAME}"
                // Thêm build thật nếu cần
                sh './mvnw clean install'  // Cấu hình Maven build
            }
        }

        stage('Test') {
            steps {
                echo "Running tests on ${env.BRANCH_NAME}"
                sh './mvnw test'  // Chạy các bài test
            }
        }

        stage('Jacoco Coverage Report') {
            steps {
                echo "Generating Jacoco coverage report"
                // Thêm bước để tạo báo cáo Jacoco
                sh './mvnw jacoco:report'
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
            echo "Build & test succeeded on branch: ${env.BRANCH_NAME}"
        }
        failure {
            echo "Build or test failed on branch: ${env.BRANCH_NAME}"
        }
    }
}
