pipeline {
    agent any

    triggers {
        githubPush()  // Thêm trigger này để nhận sự kiện từ GitHub
        genericWebhookTrigger('.*') // Thêm trigger này để nhận sự kiện từ GitHub
    }

    environment {
        // Bạn có thể thêm biến môi trường ở đây nếu cần
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
                // Thêm lệnh build thật ở đây, ví dụ: ./mvnw clean install
            }
        }

        stage('Test') {
            steps {
                echo "Running tests on ${env.BRANCH_NAME}"
                // ./mvnw test
            }
        }

        // Có thể thêm logic tùy nhánh
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo "Deploying production build!"
                // Lệnh deploy thực tế
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