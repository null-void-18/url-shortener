pipeline {
    agent any

    tools {
        maven 'maven-3'
    }

    stages {

        stage('Build') {
            steps {
                dir('urlshortener') {
                    sh 'mvn clean compile'
                }
            }
        }

        stage('Test') {
            steps {
                dir('urlshortener') {
                    sh 'mvn test'
                }
            }
        }

        stage('Package') {
            steps {
                dir('urlshortener') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Deploy') {
            steps {
                dir('urlshortener') {
                    sh '''
                    docker compose down || true
                    docker compose up --build -d

                    sleep 20

                    docker compose ps
                    '''
                }
            }
        }
    }
}
