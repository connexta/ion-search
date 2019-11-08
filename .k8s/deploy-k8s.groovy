//Groovy scripts throw lots of WARNING messages, but it is fine
//https://issues.apache.org/jira/browse/GROOVY-8339


def DOCKER_REG = System.getenv("DOCKER_REGISTRY")
if (!DOCKER_REG) {
    println "DOCKER_REGISTRY environment variable not found!"
    System.exit(1)
}

//The name of the preface for the docker images
def DOCKER_IMAGE_NAME = "cnxta/ion-search"
def DOCKER_SOLR_IMAGE = "cnxta/search-solr"

//The path to the Docker Wrapper
//IMPORTANT!
//>>> The base docker script has to be in the same location as this file

// docker_wrapper or set_docker_w if docker_wrapper isn't set
def DOCKER_W = System.getenv("DOCKER_WRAPPER")
if (!DOCKER_W) {
    println "DOCKER_WRAPPER environment variable not found!"
    System.exit(1)
}

//Tests that variables are correct for the docker compose and docker wrapper
def checkVars(docker_w) {
    def dir = System.properties['user.dir']
    def manifestFile = new File(dir,"search-deployment.yml")
    def wrapperFile = new File(docker_w)
    if (!manifestFile.exists()){
        println "Please run in the same directory as search-deployment.yml"
        System.exit(1)
    } else if (!wrapperFile.exists()){
        println docker_w + " does not exist"
        System.exit(1)
    } else if (!wrapperFile.canExecute()){
        println docker_w + " is not executable"
        System.exit(1)
    }
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

checkVars(DOCKER_W)


run("Tagging Docker images with: " + DOCKER_REG)
run("docker tag " + DOCKER_IMAGE_NAME + " " + DOCKER_REG + "/" + DOCKER_IMAGE_NAME)
run("docker push " + DOCKER_REG + "/" + DOCKER_IMAGE_NAME)

run("docker tag " + DOCKER_SOLR_IMAGE + " " + DOCKER_REG + "/" + DOCKER_SOLR_IMAGE)
run("docker push " + DOCKER_REG + "/" + DOCKER_SOLR_IMAGE)

header("Deploying the application on Kubernetes Cluster")
run("kubectl apply -f search-deployment.yml")
