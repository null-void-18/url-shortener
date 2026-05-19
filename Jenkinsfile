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
                    pkill -f 'urlshortener.*jar' || true
                    nohup java -jar target/urlshortener-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                    '''
                }
            }
        }
    }
}
