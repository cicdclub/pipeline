#!/bin/groovy
def call(){
    pipeline {
        environment {
            config_file='pipeline/config'
        }
        agent {
            label "docker"
        }
        stages {
            stage('SETUP') {
                steps {
                    script{
                        config_file = load "$config_file"
                    }
                    echo "Login to ${config_file.DOCKER_CRED} docker registry..."
                    withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: "${config_file.DOCKER_CRED}",
                        usernameVariable: "DOCKER_USR",
                        passwordVariable: "DOCKER_PWD"]])
                    {
                        sh "sudo docker login -u ${DOCKER_USR} -p ${DOCKER_PWD} ${config_file.DOCKER_REG}"
                    }
                }
            }
            stage('BUILD') {
                steps {
                    echo "Building ${config_file.DOCKER_REPO} docker image..."
                    sh "sudo docker build -t ${config_file.DOCKER_REG}/${config_file.DOCKER_REPO}:${BUILD_NUMBER} ."
                }
            }
            stage('PUSH') {
                steps {
                    echo "Pushing ${config_file.DOCKER_REPO} docker image..."
                    sh "sudo docker push ${config_file.DOCKER_REG}/${config_file.DOCKER_REPO}:${BUILD_NUMBER}"
                    sh "sudo docker tag ${config_file.DOCKER_REG}/${config_file.DOCKER_REPO}:${BUILD_NUMBER} ${config_file.DOCKER_REG}/${config_file.DOCKER_REPO}:latest"
                    sh "sudo docker push ${config_file.DOCKER_REG}/${config_file.DOCKER_REPO}:latest"
                }
            }
            stage('CLEAN') {
                steps {
                    echo "Clean up docker images older than 7 days from host..."
                    sh "sudo docker image prune -a --force --filter \"until=168h\""
                }
            }
        }
        post {
            always {
                cleanWs()
                echo 'Cleaned Up Workspace'
            }
        }
    }
}
return this;
