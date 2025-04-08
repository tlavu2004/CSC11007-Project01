pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo "Building.."
                sh '''
                    echo "test.."
                '''
                exit 1
            }
        }
        stage('Test') {
            steps {
                echo "Testing.."
            }
        }
        stage('Deliver') {
            steps {
                echo 'Deliver....'
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
        stage('Post') {
            steps {
                echo 'Post build actions...'
            }
        }
        stage('Notify') {
            steps {
                echo 'Notifying...'
            }
        }
        stage('Archive') {
            steps {
                echo 'Archiving...'
            }
        }
        stage ('Report') {
            steps {
                echo 'Reporting...'
            }
        }
    }
    post {
        always {
            echo 'This will always run'
        }
        success {
            echo 'This will run only if the build is successful'
        }
        failure {
            echo 'This will run only if the build fails'
        }
        unstable {
            echo 'This will run only if the build is unstable'
        }
        changed {
            echo 'This will run only if the build status has changed'
        }
    }
}
