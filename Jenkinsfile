pipeline {
    agent any

    tools {
        maven 'maven-3'
    }

    stages {

        stage('Build') {
            steps {
                dir('url-shortener') {
                    sh 'mvn clean compile'
                }
            }
        }

        stage('Test') {
            steps {
                dir('url-shortener') {
                    sh 'mvn test'
                }
            }
        }
    }
}
