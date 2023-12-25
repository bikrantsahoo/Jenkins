pipeline {
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Select a  deployment environment')
    }

    agent any

    environment {
        KUBE_NAMESPACE = ''
        KUBE_DEPLOYMENT_NAME = ''
        KUBE_CONTAINER_NAME = ''
        KUBE_SERVER = ''
        KUBE_CREDENTIALS_ID = ''
    }

    stages {
        stage('Read Configuration') {
            steps {
                script {
                    def configFile = "config-${params.ENVIRONMENT}.yml"
                    def config = readYaml file: configFile

                    // Set environment variables based on the configuration
                    KUBE_NAMESPACE = config.kubeNamespace
                    KUBE_DEPLOYMENT_NAME = config.kubeDeploymentName
                    KUBE_CONTAINER_NAME = config.kubeContainerName
                    KUBE_SERVER = config.kubeServer
                    KUBE_CREDENTIALS_ID = config.kubeCredentialsId
                }
            }
        }

        stage('Git Checkout') {
            steps {
                script {
                    git branch: config.gitBranch, credentialsId: config.gitCredentialsId, url: config.gitRepoUrl
                }
            }
        }

        stage('Code Scanning') {
            steps {
                script {
                    withSonarQubeEnv(config.sonarServerId) {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
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
                    sh 'docker build -t your-image-name .'
                }
            }
        }

        stage('Docker Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: config.registryCredentialsId, usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD ${config.registryUrl}"
                        sh 'docker push ${config.registryUrl}/your-image-name'
                    }
                }
            }
        }

        stage('Canary Deploy to Kubernetes') {
            steps {
                script {
                    def canaryImage = "${config.registryUrl}/your-image-name:canary"
                    sh "kubectl set image deployment/${KUBE_DEPLOYMENT_NAME} ${KUBE_CONTAINER_NAME}=${canaryImage} --namespace=${KUBE_NAMESPACE}"
                }
            }
        }

        stage('Promote Canary to Production') {
            steps {
                script {
                    def productionImage = "${config.registryUrl}/<image-name:latest>"
                    sh "kubectl set image deployment/${KUBE_DEPLOYMENT_NAME} ${KUBE_CONTAINER_NAME}=${productionImage} --namespace=${KUBE_NAMESPACE}"
                }
            }
        }
    }
}
