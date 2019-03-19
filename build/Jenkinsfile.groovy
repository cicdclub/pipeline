#!/bin/groovy
def call(){
    pipeline {
        environment {
            config_file='pipeline/config.groovy'
            DOCKER_CRED = ''
            DOCKER_REPO = ''
            DOCKER_REG = ''
        }
        agent {
            label "master"
        }
        stages {
            stage('TEMP') {
                steps {
                    script{
                        config_file = load "$config_file"
                        def DOCKER_CRED = config_file.DOCKER_CRED
                        sh "echo ${DOCKER_CRED}"
                    }
                }
            }
            stage('TEMP1') {
                steps {
                    script{
                        def DOCKER_REPO = config_file.DOCKER_REPO
                        sh "echo ${DOCKER_REPO}"
                    }
                }
            }
            stage('TEMP2') {
                steps {
                    sh "test2"
                    sh "echo ${DOCKER_REPO}"
                }
            }
            stage('TEMP3') {
                steps {
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
