#!/bin/groovy
def call(){
    pipeline {
        environment {
            config_file='pipeline/config.groovy'
        }
        agent {
            label "master"
        }
        stages {
            stage('TEMP') {
                steps {
                    script {
                        config_file = load "$config_file"
                        def DOCKER_CRED = config_file.DOCKER_CRED
                        def DOCKER_REPO = config_file.DOCKER_REPO
                        echo "DOCKER_CRED: ${DOCKER_CRED}"
                        echo "DOCKER_REPO: ${DOCKER_REPO}"
                        echo "DOCKER_REG: ${config_file.DOCKER_REG}"
                    }
                }
            }
            stage('TEMP1') {
                steps {
                    echo "DOCKER_CRED: ${DOCKER_CRED}"
                    echo "DOCKER_REPO: ${DOCKER_REPO}"
                    echo "DOCKER_REG: ${config_file.DOCKER_REG}"
                    sh "exit 1"
                }
            }
            stage('SETUP') {
                steps {
                    echo "Login to ${DOCKER_REPO} docker registry..."
                    withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: "${DOCKER_CRED}",
                        usernameVariable: "DOCKER_USR",
                        passwordVariable: "DOCKER_PWD"]])
                    {
                        sh "sudo docker login -u ${DOCKER_USR} -p ${DOCKER_PWD} ${DOCKER_REG}"
                    }
                }
            }
            stage('BUILD') {
                steps {
                    echo "Building ${DOCKER_REPO} docker image..."
                    sh "sudo docker build -t ${DOCKER_REG}/${DOCKER_REPO}:${BUILD_NUMBER} ."
                }
            }
            stage('PUSH') {
                steps {
                    echo "Pushing ${DOCKER_REPO} docker image..."
                    sh "sudo docker push ${DOCKER_REG}/${DOCKER_REPO}:${BUILD_NUMBER}"
                    sh "sudo docker tag ${DOCKER_REG}/${DOCKER_REPO}:${BUILD_NUMBER} ${DOCKER_REG}/${DOCKER_REPO}:latest"
                    sh "sudo docker push ${DOCKER_REG}/${DOCKER_REPO}:latest"
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
                script {
                  def credentials = '/root/.aws/credentials'
                  if(fileExists(credentials))
                  {
                    sh "rm /root/.aws/credentials"
                  }

                }
            }
            success {
                cleanWs()
                echo 'Cleaned Up Workspace'
            }
        }
    }
}
return this;
