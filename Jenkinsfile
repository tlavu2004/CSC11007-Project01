pipeline {
    agent any

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
                sh './mvnw clean install'
            }
        }

        stage('Test') {
            steps {
                echo "Running tests on ${env.BRANCH_NAME}"
            }
        }

        stage('Jacoco Coverage Report') {
            steps {
                echo "Generating Jacoco coverage report"
                echo "Report should be available in target/site/jacoco/index.html"
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
        always {
            publishCoverage adapters: [jacocoAdapter('**/target/site/jacoco/jacoco.xml')]
        }

        success {
            echo "Build & test succeeded on branch: ${env.BRANCH_NAME}"
        }
        failure {
            echo "Build or test failed on branch: ${env.BRANCH_NAME}"
        }
    }
}
