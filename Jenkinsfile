pipeline {
    agent any
    
    environment {
        // Thay đổi Username dockerhub của bạn ở đây
        DOCKER_REGISTRY = 'gianguyen97'
        PROJECT_VERSION = "${env.BUILD_NUMBER}"
        // Khai báo credential ID đã thiết lập trên Jenkins
        DOCKER_CREDENTIAL = 'docker-hub-credentials'
    }

    stages {
        stage('Checkout Code') {
            steps {
                echo "Pulling latest code from repository..."
                checkout scm
            }
        }

        stage('Build & Test Backend Services') {
            steps {
                echo "Building Async Service..."
                dir('backend/async-service') {
                    bat 'gradlew.bat clean build -x test'
                }
                
                echo "Building Direct Service..."
                dir('backend/direct-service') {
                    bat 'gradlew.bat clean build -x test'
                }
                
                echo "Building Kafka Service..."
                dir('backend/kafka-service') {
                    bat 'gradlew.bat clean build -x test'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    echo "Building Backend Docker Images..."
                    docker.build("${DOCKER_REGISTRY}/async-service:${PROJECT_VERSION}", "-f backend/async-service/Dockerfile backend/async-service")
                    docker.build("${DOCKER_REGISTRY}/direct-service:${PROJECT_VERSION}", "-f backend/direct-service/Dockerfile backend/direct-service")
                    docker.build("${DOCKER_REGISTRY}/kafka-service:${PROJECT_VERSION}", "-f backend/kafka-service/Dockerfile backend/kafka-service")
                    
                    echo "Building Frontend Docker Image..."
                    docker.build("${DOCKER_REGISTRY}/stress-frontend:${PROJECT_VERSION}", "-f frontend/Dockerfile frontend")
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    echo "Pushing Images to Docker Hub..."
                    docker.withRegistry('https://index.docker.io/v1/', DOCKER_CREDENTIAL) {
                        docker.image("${DOCKER_REGISTRY}/async-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/direct-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/kafka-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/stress-frontend:${PROJECT_VERSION}").push()
                    }
                }
            }
        }

        stage('Deploy to Production') {
            steps {
                echo "Deploying Locally..."
                withEnv(["IMAGE_REGISTRY=${DOCKER_REGISTRY}", "IMAGE_TAG=${PROJECT_VERSION}"]) {
                    dir('docker') {
                        bat '''
                            docker-compose -f docker-compose.prod.yml pull
                            docker-compose -f docker-compose.prod.yml up -d --remove-orphans
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo "CI/CD Pipeline Finished!"
            // Có thể cấu hình gửi thông báo Slack/Email ở đây
        }
        success {
            echo "Deployment was super successful."
        }
        failure {
            echo "Deployment failed. Check logs."
        }
    }
}
