pipeline {
    agent any

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Select deployment environment')
    }

    stages {
        stage('Read Configuration') {
            steps {
                script {
                    // Read configuration from YAML file based on the selected environment
                    def configFile = "config-${params.ENVIRONMENT}.yml"
                    def config = readYaml file: configFile

                    // Set environment variables based on the configuration
                    env.KUBE_NAMESPACE = config.kubeNamespace
                    env.KUBE_DEPLOYMENT_NAME = config.kubeDeploymentName
                    env.KUBE_CONTAINER_NAME = config.kubeContainerName
                    env.KUBE_SERVER = config.kubeServer
                    env.KUBE_CREDENTIALS_ID = config.kubeCredentialsId
                }
            }
        }

        stage('Execute Python Script') {
            steps {
                script {
                    // Execute the Python script
                    sh 'python pipeline_script.py'
                }
            }
        }
    }
}
