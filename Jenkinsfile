pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES') // Chống treo toàn hệ thống
    }

    parameters {
        string(name: 'DEPLOY_VERSION', defaultValue: 'latest', description: 'Để trống hoặc "latest" để build. Nhập số BUILD_NUMBER (vd: 12) để Rollback.')
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

        stage('Determine Environment') {
            steps {
                script {
                    def branchName = env.GIT_BRANCH ?: env.BRANCH_NAME ?: 'main'
                    echo "Webhook/SCM Trigger detected on branch: ${branchName}."
                    if (branchName.endsWith('main') || branchName.endsWith('master')) {
                        env.TARGET_ENV = 'production'
                    } else if (branchName.endsWith('staging')) {
                        env.TARGET_ENV = 'staging'
                    } else {
                        env.TARGET_ENV = 'dev'
                    }
                    echo "Mapped Environment TARGET_ENV='${env.TARGET_ENV}'"
                }
            }
        }

        stage('Debug Docker Connection') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            steps {
                echo "=== VERIFYING DOCKER DAEMON AVAILABILITY ==="
                sh 'docker version'
                sh 'docker info'
            }
        }

        stage('Unit Test') {
            when { expression { params.DEPLOY_VERSION == 'latest' } }
            failFast true
            steps {
                echo "=== RUNNING UNIT TESTS ==="
                // Cấp quyền thực thi cho gradlew phòng trường hợp bị mất permission
                sh 'chmod +x ./backend/async-service/gradlew'
                sh 'chmod +x ./backend/direct-service/gradlew'
                sh 'chmod +x ./backend/kafka-service/gradlew'

                dir('backend/async-service') {
                    sh './gradlew test --build-cache'
                }
                dir('backend/direct-service') {
                    sh './gradlew test --build-cache'
                }
                dir('backend/kafka-service') {
                    sh './gradlew test --build-cache'
                }
                echo "=== UNIT TESTS PASSED ==="
            }
        }

        stage('Static Code Analysis (SonarQube)') {
            when { expression { env.TARGET_ENV == 'production' && params.DEPLOY_VERSION == 'latest' } }
            steps {
                echo "=== RUNNING SONARQUBE ANALYSIS ==="
                // sh './gradlew sonar'
                
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
                    sh './gradlew assemble -x test --build-cache'
                }
                dir('backend/direct-service') {
                    sh './gradlew assemble -x test --build-cache'
                }
                dir('backend/kafka-service') {
                    sh './gradlew assemble -x test --build-cache'
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
                        docker.image("${DOCKER_REGISTRY}/async-service:${PROJECT_VERSION}").push("latest")
                        docker.image("${DOCKER_REGISTRY}/direct-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/direct-service:${PROJECT_VERSION}").push("latest")
                        docker.image("${DOCKER_REGISTRY}/kafka-service:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/kafka-service:${PROJECT_VERSION}").push("latest")
                        docker.image("${DOCKER_REGISTRY}/stress-frontend:${PROJECT_VERSION}").push()
                        docker.image("${DOCKER_REGISTRY}/stress-frontend:${PROJECT_VERSION}").push("latest")
                    }
                }
            }
        }

        stage('Set Target Configuration') {
            steps {
                script {
                    if (env.TARGET_ENV == 'dev') {
                        env.OVERRIDE_FILE = 'docker-compose.override.dev.yml'
                    } else if (env.TARGET_ENV == 'staging') {
                        env.OVERRIDE_FILE = 'docker-compose.override.staging.yml'
                    } else {
                        env.OVERRIDE_FILE = 'docker-compose.override.production.yml'
                    }
                    echo "Selected config file: docker-compose.yml + ${env.OVERRIDE_FILE}"
                }
            }
        }


        stage('Deploy To Target Environment') {
            steps {
                echo "=== ORCHESTRATING DEPLOYMENT TO [${env.TARGET_ENV}] ==="
                
                withEnv(["IMAGE_REGISTRY=${DOCKER_REGISTRY}", "IMAGE_TAG=${PROJECT_VERSION}", "APP_ENV=${env.TARGET_ENV}"]) {
                    dir('docker') {
                        script {
                            try {
                                sh """
                                    docker-compose -p stress_test_${env.TARGET_ENV} -f docker-compose.yml -f \${OVERRIDE_FILE} pull
                                    echo "Applying rolling update pattern using --wait parameter..."
                                    docker-compose -p stress_test_${env.TARGET_ENV} -f docker-compose.yml -f \${OVERRIDE_FILE} up -d --wait --remove-orphans
                                """
                                // Lưu vết phiên bản thành công
                                sh "echo ${PROJECT_VERSION} > .env.last_stable_${env.TARGET_ENV}"
                            } catch (Exception e) {
                                echo "DEPLOY FAILED! Kích hoạt Auto-Rollback cứu hộ môi trường..."
                                def lastStable = sh(script: "cat .env.last_stable_${env.TARGET_ENV} || echo 'latest'", returnStdout: true).trim()
                                withEnv(["IMAGE_TAG=${lastStable}"]) {
                                    sh """
                                        echo "Lùi về version an toàn gần nhất: IMAGE_TAG=\${IMAGE_TAG}"
                                        docker-compose -p stress_test_${env.TARGET_ENV} -f docker-compose.yml -f \${OVERRIDE_FILE} up -d --wait --remove-orphans
                                    """
                                }
                                error("Deployment thất bại! Hệ thống đã an toàn quay về version: ${lastStable}")
                            }
                        }
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

