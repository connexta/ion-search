#!/usr/bin/env groovy
//Groovy scripts throw lots of WARNING messages, but it is fine
//https://issues.apache.org/jira/browse/GROOVY-8339

def DOCKER_REG = System.getenv("K8S_DOCKER_REGISTRY") + "/"
def DOCKER_IMAGE_NAME = "cnxta/ion-search"
def DOCKER_SOLR_IMAGE = "cnxta/search-solr"
def K8S_CONTEXT = getKubeContext()


def getKubeContext() {
    return "kubectl config current-context".execute().text.trim()
}

def run(commands) {
    println "Running: " + commands
    def proc = commands.execute()
    proc.waitForProcessOutput(System.out, System.err)
}

def header(message) {
    println ""
    println "# # # # # # # # # # # # # #"
    println " " + message
    println ""
}

header("Tagging images...")
run("docker tag " + DOCKER_IMAGE_NAME + " " + DOCKER_REG + DOCKER_IMAGE_NAME)
run("docker tag " + DOCKER_SOLR_IMAGE + " " + DOCKER_REG + DOCKER_SOLR_IMAGE)

if (K8S_CONTEXT != "minikube") {
    header("Pushing image to remote registry")
    run("docker push " + DOCKER_REG + DOCKER_IMAGE_NAME)
    run("docker push " + DOCKER_REG + DOCKER_SOLR_IMAGE)
}

header("Creating configMaps for Search Service...")
run("kubectl create configmap search-config-map --from-file=./.k8s/k8s_search_config.yml")

header("Deploying...")
run("kubectl apply -f ./.k8s/search-deployment.yml")
