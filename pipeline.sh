pipeline {
    agent any
    
    environment {
        KUBE_NAMESPACE = '<namespace-name>'
        KUBE_DEPLOYMENT_NAME = '<deployment-name>'
        KUBE_CONTAINER_NAME = '<container-name>'
        KUBE_SERVER = '<-kube-api-server>'
        KUBE_CREDENTIALS_ID = '<kube-credentials-id>'
    }

    stages {
        stage('Git Checkout') {
            steps {
                // Checkout your source code from Git
                script {
                    git branch: '<branch-name>', credentialsId: '<git-credentials-id>', url: 'https://git-repo-url.git'
                }
            }
        }

        stage('Code Scanning') {
            steps {
                // Perform code scanning using SonarQube
                script {
                    withSonarQubeEnv('<sonarqube-server-id>') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                // Run unit tests
                script {
                    sh 'mvn test'
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    sh 'mvn clean install'
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    // Build  Docker image
                    sh 'docker build -t your-image-name .'
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    // Push the Docker image to a container registry
                    withCredentials([usernamePassword(credentialsId: '<registry-credentials-id>', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD <registry-url>"
                        sh 'docker push your-registry-url/your-image-name'
                    }
                }
            }
        }

        stage('Canary Deploy to Kubernetes') {
            steps {
                script {
                    // Canary deployment to Kubernetes
                    def canaryImage = "your-registry-url/your-image-name:canary"
                    sh "kubectl set image deployment/${KUBE_DEPLOYMENT_NAME} ${KUBE_CONTAINER_NAME}=${canaryImage} --namespace=${KUBE_NAMESPACE}"
                }
            }
        }

        stage('Promote Canary to Production') {
            steps {
                script {
                    // Promote the Canary deployment to production
                    def productionImage = "<registry-url>/<image-name:latest>"
                    sh "kubectl set image deployment/${KUBE_DEPLOYMENT_NAME} ${KUBE_CONTAINER_NAME}=${productionImage} --namespace=${KUBE_NAMESPACE}"
                }
            }
        }
    }
}
