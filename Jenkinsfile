pipeline {
    agent any
    stages {
        stage('Determine Changed Services') {
            steps {
                script {
                    // Kiểm tra các file thay đổi
                    def changes = sh(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim()
                    echo "Changed files:\n${changes}"

                    // Kiểm tra các thư mục cụ thể và gán giá trị Boolean
                    env.API_GATEWAY_CHANGED = changes.contains("spring-petclinic-api-gateway/") ? "true" : "false"
                    env.CUSTOMERS_CHANGED = changes.contains("spring-petclinic-customers-service/") ? "true" : "false"
                    env.VETS_CHANGED = changes.contains("spring-petclinic-vets-service/") ? "true" : "false"
                    env.VISITS_CHANGED = changes.contains("spring-petclinic-visits-service/") ? "true" : "false"

                    echo "API_GATEWAY_CHANGED=${env.API_GATEWAY_CHANGED}"
                    echo "CUSTOMERS_CHANGED=${env.CUSTOMERS_CHANGED}"
                    echo "VETS_CHANGED=${env.VETS_CHANGED}"
                    echo "VISITS_CHANGED=${env.VISITS_CHANGED}"
                }
            }
        }

        stage('Build and Test API Gateway') {
            when {
                expression { env.API_GATEWAY_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing API Gateway"
                dir('spring-petclinic-api-gateway') {
                    sh './mvnw clean install'
                }
            }
        }

        stage('Build and Test Customers Service') {
            when {
                expression { env.CUSTOMERS_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing Customers Service"
                dir('spring-petclinic-customers-service') {
                    sh './mvnw clean install'
                }
            }
        }

        stage('Build and Test Vets Service') {
            when {
                expression { env.VETS_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing Vets Service"
                dir('spring-petclinic-vets-service') {
                    sh './mvnw clean install'
                }
            }
        }

        stage('Build and Test Visits Service') {
            when {
                expression { env.VISITS_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing Visits Service"
                dir('spring-petclinic-visits-service') {
                    sh './mvnw clean install'
                }
            }
        }
    }
}
