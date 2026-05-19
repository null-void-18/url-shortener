pipeline {
    agent any

    tools {
        maven 'maven-3'
    }

    stages {

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                pkill -f 'urlshortener.*jar' || true
                nohup java -jar target/urlshortener-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                '''
            }
        }
    }
}