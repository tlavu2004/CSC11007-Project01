pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
            exit 1
        }
        stage('Test') {
            steps {
                echo 'Testing...'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying...'
            }
        }
        stage('Cleanup') {
            steps {
                echo 'Cleaning up...'
            }
        }
        stage('Notify') {
            steps {
                echo 'Notifying...'
            }
        }
    }
}