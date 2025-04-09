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
                    env.ADMIN_SERVER_CHANGED = changes.contains("spring-petclinic-admin-server/") ? "true" : "false"
                    env.DISCOVERY_SERVER_CHANGED = changes.contains("spring-petclinic-discovery-server/") ? "true" : "false"
                    env.CONFIG_SERVER_CHANGED = changes.contains("spring-petclinic-config-server/") ? "true" : "false"
                    env.GENAI_SERVICE_CHANGED = changes.contains("spring-petclinic-genai-service/") ? "true" : "false"

                    echo "API_GATEWAY_CHANGED=${env.API_GATEWAY_CHANGED}"
                    echo "CUSTOMERS_CHANGED=${env.CUSTOMERS_CHANGED}"
                    echo "VETS_CHANGED=${env.VETS_CHANGED}"
                    echo "VISITS_CHANGED=${env.VISITS_CHANGED}"
                    echo "ADMIN_SERVER_CHANGED=${env.ADMIN_SERVER_CHANGED}"
                    echo "DISCOVERY_SERVER_CHANGED=${env.DISCOVERY_SERVER_CHANGED}"
                    echo "CONFIG_SERVER_CHANGED=${env.CONFIG_SERVER_CHANGED}"
                    echo "GENAI_SERVICE_CHANGED=${env.GENAI_SERVICE_CHANGED}"
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
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
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
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
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
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
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
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build and Test Admin Server') {
            when {
                expression { env.ADMIN_SERVER_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing Admin Server"
                dir('spring-petclinic-admin-server') {
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build and Test Discovery Server') {
            when {
                expression { env.DISCOVERY_SERVER_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing Discovery Server"
                dir('spring-petclinic-discovery-server') {
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build and Test Config Server') {
            when {
                expression { env.CONFIG_SERVER_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing Config Server"
                dir('spring-petclinic-config-server') {
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build and Test GenAI Service') {
            when {
                expression { env.GENAI_SERVICE_CHANGED.toBoolean() }
            }
            steps {
                echo "Building and testing GenAI Service"
                dir('spring-petclinic-genai-service') {
                    sh 'mvn clean install'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }
}
