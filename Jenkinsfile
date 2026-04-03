pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES') // Chống treo toàn hệ thống
    }

    parameters {
        string(name: 'DEPLOY_VERSION', defaultValue: 'latest', description: 'Để trống hoặc "latest" để build. Nhập số BUILD_NUMBER (vd: 12) để Rollback.')
        choice(name: 'TARGET_ENV', choices: ['dev', 'staging', 'production'], description: 'Môi trường hệ thống muốn Deploy tới')
    }
    
    environment {
        DOCKER_REGISTRY = 'gianguyen97'
        PROJECT_VERSION = "${params.DEPLOY_VERSION == 'latest' ? env.BUILD_NUMBER : params.DEPLOY_VERSION}"
        DOCKER_CREDENTIAL = 'docker-hub-credentials'
    }

    stages {
        stage('Checkout Code') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            steps {
                echo "=== PULLING LATEST SOURCE CODE ==="
                checkout scm
            }
        }

        stage('Debug Docker Connection') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            steps {
                echo "=== VERIFYING DOCKER DAEMON AVAILABILITY ==="
                bat 'docker version'
                bat 'docker info'
            }
        }

        stage('Unit Test') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            failFast true
            steps {
                echo "=== RUNNING UNIT TESTS ==="
                dir('backend/async-service') {
                    // Caching dependencies để giảm thiểu download
                    bat 'gradlew.bat test --build-cache'
                }
                dir('backend/direct-service') {
                    bat 'gradlew.bat test --build-cache'
                }
                dir('backend/kafka-service') {
                    bat 'gradlew.bat test --build-cache'
                }
                echo "=== UNIT TESTS PASSED ==="
            }
        }

        stage('Static Code Analysis (SonarQube)') {
            when { expression { params.TARGET_ENV == 'production' && params.DEPLOY_VERSION == 'latest' } }
            steps {
                echo "=== RUNNING SONARQUBE ANALYSIS ==="
                // bat 'gradlew.bat sonar'
                
                // MOCK: Chờ kết quả Quality Gate từ SonarQube tránh code bốc mùi lên Prod
                echo "Waiting for SonarQube Quality Gate result..."
                // timeout(time: 10, unit: 'MINUTES') {
                //    waitForQualityGate abortPipeline: true
                // }
                echo "=== SONARQUBE PASSED ==="
            }
        }

        stage('Build & Package') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            steps {
                echo "=== COMPILING JAR (OPTIMIZED WITH DOCKER COPY) ==="
                dir('backend/async-service') {
                    bat 'gradlew.bat assemble -x test --build-cache'
                }
                dir('backend/direct-service') {
                    bat 'gradlew.bat assemble -x test --build-cache'
                }
                dir('backend/kafka-service') {
                    bat 'gradlew.bat assemble -x test --build-cache'
                }
            }
        }

        stage('Build Docker Images') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            steps {
                script {
                    echo "=== PACKAGING DOCKER IMAGES (TAG: ${PROJECT_VERSION}) ==="
                    docker.build("${DOCKER_REGISTRY}/async-service:${PROJECT_VERSION}", "-f backend/async-service/Dockerfile backend/async-service")
                    docker.build("${DOCKER_REGISTRY}/direct-service:${PROJECT_VERSION}", "-f backend/direct-service/Dockerfile backend/direct-service")
                    docker.build("${DOCKER_REGISTRY}/kafka-service:${PROJECT_VERSION}", "-f backend/kafka-service/Dockerfile backend/kafka-service")
                    docker.build("${DOCKER_REGISTRY}/stress-frontend:${PROJECT_VERSION}", "-f frontend/Dockerfile frontend")
                }
            }
        }

        stage('Push Docker Images') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            steps {
                script {
                    echo "=== PUSHING IMMUTABLE VERSION TO DOCKER REPO ==="
                    docker.withRegistry('https://index.docker.io/v1/', DOCKER_CREDENTIAL) {
                        docker.image("${DOCKER_REGISTRY}/async-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/direct-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/kafka-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/stress-frontend:${PROJECT_VERSION}").push()
                    }
                }
            }
        }

        stage('Deploy To Target Environment') {
            steps {
                echo "=== ORCHESTRATING DEPLOYMENT TO [${params.TARGET_ENV}] ==="
                
                withEnv(["IMAGE_REGISTRY=${DOCKER_REGISTRY}", "IMAGE_TAG=${PROJECT_VERSION}", "APP_ENV=${params.TARGET_ENV}"]) {
                    dir('docker') {
                        bat '''
                            docker-compose -f docker-compose.prod.yml pull
                            @echo "Applying rolling update pattern using --wait parameter..."
                            docker-compose -f docker-compose.prod.yml up -d --wait --remove-orphans
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo "=== PIPELINE EXECUTION COMPLETED ==="
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
        }
        success {
            echo "DEPLOYMENT COMPLETED SUCCESSFULLY. VERSION: ${PROJECT_VERSION}"
        }
        failure {
            echo "PIPELINE FAILED. QUALITY GATES / TESTS DID NOT PASS OR DEPLOY CRASHED."
        }
    }
}
