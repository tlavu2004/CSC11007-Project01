pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
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
        stage('Post') {
            steps {
                echo 'Post-processing...'
            }
        }
        stage  ('Archice') {
            steps {
                echo 'Archiving...'
            }
        }
    }
    post {
        success {
            githubNotify context: 'CI', status: 'SUCCESS', description: 'Build passed!'
        }
        failure {
            githubNotify context: 'CI', status: 'FAILURE', description: 'Build failed!'
        }
    }
}