import subprocess
import yaml

def read_configuration(environment):
    config_file = f'config-{environment}.yml'
    with open(config_file, 'r') as file:
        config = yaml.safe_load(file)
    return config

def git_checkout(branch, credentials_id, repo_url):
    subprocess.run(['git', 'checkout', '-b', branch, '-c', credentials_id, repo_url])

def code_scanning():
    subprocess.run(['mvn', 'sonar:sonar'])

def run_unit_tests():
    subprocess.run(['mvn', 'test'])

def build():
    subprocess.run(['mvn', 'clean', 'install'])

def docker_build():
    subprocess.run(['docker', 'build', '-t', 'your-image-name', '.'])

def docker_push(registry_credentials_id, registry_url):
    subprocess.run(['docker', 'login', '-u', '$DOCKER_USERNAME', '-p', '$DOCKER_PASSWORD', registry_url])
    subprocess.run(['docker', 'push', f'{registry_url}/your-image-name'])

def kubernetes_deploy(deployment_name, container_name, image, namespace):
    subprocess.run(['kubectl', 'set', 'image', f'deployment/{deployment_name}', f'{container_name}={image}', f'--namespace={namespace}'])

def main():
    environment = 'dev'  # You can pass this as a parameter or read from user input
    config = read_configuration(environment)

    git_checkout(config['git_branch'], config['git_credentials_id'], config['git_repo_url'])
    code_scanning()
    run_unit_tests()
    build()
    docker_build()
    docker_push(config['registry_credentials_id'], config['registry_url'])

    # For Canary Deploy
    canary_image = f"{config['registry_url']}/your-image-name:canary"
    kubernetes_deploy(config['kube_deployment_name'], config['kube_container_name'], canary_image, config['kube_namespace'])

    # For Production Deploy
    production_image = f"{config['registry_url']}/<image-name:latest>"
    kubernetes_deploy(config['kube_deployment_name'], config['kube_container_name'], production_image, config['kube_namespace'])

if __name__ == "__main__":
    main()
