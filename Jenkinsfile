pipeline {
    agent any
    stages {
        stage('Determine Changed Services') {
            steps {
                script {
                    // Kiểm tra các file thay đổi
                    def changes = sh(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim()
                    echo "Changed files:\n${changes}"

                    // Kiểm tra các thư mục cụ thể
                    env.API_GATEWAY_CHANGED = changes.contains("spring-petclinic-api-gateway/")
                    env.CUSTOMERS_CHANGED = changes.contains("spring-petclinic-customers-service/")
                    env.VETS_CHANGED = changes.contains("spring-petclinic-vets-service/")
                    env.VISITS_CHANGED = changes.contains("spring-petclinic-visits-service/")
                }
            }
        }

        stage('Build and Test API Gateway') {
            when {
                expression { env.API_GATEWAY_CHANGED == "true" }
            }
            steps {
                dir('spring-petclinic-api-gateway') {
                    sh './mvnw clean install'
                }
            }
        }

        stage('Build and Test Customers Service') {
            when {
                expression { env.CUSTOMERS_CHANGED == "true" }
            }
            steps {
                dir('spring-petclinic-customers-service') {
                    sh './mvnw clean install'
                }
            }
        }

        stage('Build and Test Vets Service') {
            when {
                expression { env.VETS_CHANGED == "true" }
            }
            steps {
                dir('spring-petclinic-vets-service') {
                    sh './mvnw clean install'
                }
            }
        }

        stage('Build and Test Visits Service') {
            when {
                expression { env.VISITS_CHANGED == "true" }
            }
            steps {
                dir('spring-petclinic-visits-service') {
                    sh './mvnw clean install'
                }
            }
        }
    }
}
