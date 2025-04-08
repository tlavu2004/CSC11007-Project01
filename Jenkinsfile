pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building project...'
                sh './mvnw clean package'
            }
        }
        stage('Test') {
            steps {
                echo 'Running tests...'
                sh './mvnw test'
            }
        }
    }
}
