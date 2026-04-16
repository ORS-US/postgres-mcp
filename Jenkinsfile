pipeline {

    options {
        buildDiscarder(logRotator(numToKeepStr: '60'))
        githubProjectProperty(projectUrlStr: 'https://github.com/ORS-US/postgres-mcp')
        ansiColor('xterm')
        timestamps()
        skipDefaultCheckout true
    }

    triggers {
        githubPush()
    }

    agent { label 'kaniko-executor-amd64' }

    stages {

        stage('build') {
            steps {
                checkout scm
                container(name: 'docker', shell: '/busybox/sh') {
                    withEnv(['PATH+EXTRA=/busybox']) {
                        sh """
                        echo "Building Docker image (tag latest)"
                        /kaniko/executor \
                            --dockerfile=Dockerfile \
                            --context=${WORKSPACE} \
                            --ignore-path=/busybox \
                            --destination=iad.ocir.io/idpjbdyu47dd/action-analytics/postgres-mcp:latest \
                            --log-timestamp
                        """
                    }
                }
            }
        }

        stage('deploy') {
            agent { label 'helm-executor' }
            steps {
                checkout scm
                container('helmcontainer') {
                    script {
                        withCredentials([string(credentialsId: 'url-k8s-cluster-us', variable: 'CLUSTER_URL')]) {
                            withKubeConfig([credentialsId: 'SA-cluster-us-action-analytics-dev', serverUrl: CLUSTER_URL]) {
                                sh """
                                echo "Deploying to Kubernetes using Helm"
                                helm upgrade --install action-analytics-postgres-mcp $WORKSPACE/architecture/postgres-mcp/ \
                                    -f $WORKSPACE/architecture/postgres-mcp/values.yaml \
                                    --debug --kubeconfig $KUBECONFIG -n action-analytics-dev
                                """
                            }
                        }
                    }
                }
            }
        }
    }
}
