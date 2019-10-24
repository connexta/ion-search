//Groovy scripts throw lots of WARNING messages, but it is fine
//https://issues.apache.org/jira/browse/GROOVY-8339

import groovy.transform.Field

//The name of the base docker image
@Field
def DOCKER_IMAGE_NAME = 'cnxta/ion-search'
@Field
def STACK = 'search-stack'
@Field
def NETWORK = 'cdr'
@Field
def ENVIRONMENT = [:]

if(needHelp(args)) {
    usage()
}

@Field
def dryRun = isDryRun(args)

if(dryRun) {
    header('DRY RUN ONLY')
}

def registry = getDockerRegistry()
def dockerCommand = getDockerCommand()

if (isLocalDeployment(registry, dockerCommand)) {
    header('Deployment type: local')
    localDeployment(dockerCommand)
} else {
    header('Deployment type: non-local')
    nonLocalDeployment(registry, dockerCommand)
}

def localDeployment(dockerCommand) {
    ENVIRONMENT.IMAGE_PREFIX = ''
    verifyEnvironmentVariablesExist()
    verifyDockerComposeFileExist()
    createNetwork(dockerCommand)
    deploy(dockerCommand)
    listServices(dockerCommand)
}

def nonLocalDeployment(registry, dockerCommand) {
    ENVIRONMENT.IMAGE_PREFIX = registry + '/'
    verifyDockerCommand(dockerCommand)
    verifyEnvironmentVariablesExist()
    verifyDockerComposeFileExist()
    createNetwork(dockerCommand)
    tag(registry, dockerCommand)
    push(registry, dockerCommand)
    pull(registry, dockerCommand)
    deploy(dockerCommand)
    listServices(dockerCommand)
}

def isDryRun(args) {
    return args.length == 1 && args[0] != null && args[0].equals('--dryRun')
}

def needHelp(args) {
    return args.length == 1 && args[0] != null && args[0].equals('--help')
}

def usage() {
    println('\nUsage:')
    println('\n  Options: --help | --dryRun')
    println('\n\$> groovy deploy.groovy [options]')
    System.exit(0)
}

def getDockerRegistry() {
    return System.getenv('DOCKER_REGISTRY')
}

def getDockerCommand() {
    return System.getenv('DOCKER_WRAPPER') != null ? System.getenv('DOCKER_WRAPPER') : 'docker'
}

def verifyDockerCommand(command) {
    def dockerCommand = new File(command)
    if (!dockerCommand.exists()) {
        println dockerCommand + ' does not exist.'
        System.exit(1)
    } else if (!dockerCommand.canExecute()) {
        println dockerCommand + ' is not executable.'
        System.exit(1)
    }
}

def isLocalDeployment(registry, dockerCommand) {
    return registry == null || registry.trim().isEmpty() || dockerCommand == null || dockerCommand.trim().isEmpty()
}

def verifyDockerComposeFileExist() {
    def currentDirectory = System.properties['user.dir']
    def composeFile = new File(currentDirectory, 'docker-compose.yml')
    if (!composeFile.exists()) {
        println 'Please run in the same directory as a docker-compose.yml'
        System.exit(1)
    }
}

def tag(registry, dockerCommand) {
    def tags = DOCKER_IMAGE_NAME + ' ' + registry + '/' + DOCKER_IMAGE_NAME
    header('Tagging docker image with tags: ' + tags)
    run(dockerCommand + ' tag ' + tags)
}

def push(registry, dockerCommand) {
    header('Pushing docker image to ' + registry)
    run(dockerCommand + ' push ' + registry + '/' + DOCKER_IMAGE_NAME)
}

def pull(registry, dockerCommand) {
    header('Pulling the docker image on ' + registry)
    run(dockerCommand + ' pull ' + registry + '/' + DOCKER_IMAGE_NAME)
}

def deploy(dockerCommand) {
    header('Deploying stack ' + STACK)
    run(dockerCommand + ' stack deploy -c docker-compose.yml ' + STACK)
}

def listServices(dockerCommand) {
    header('Services in stack ' + STACK)
    run(dockerCommand + ' stack services ' + STACK)
}

def createNetwork(dockerCommand) {
    def foundNetwork = doesNetworkExist(dockerCommand)
    if(!foundNetwork) {
        header('Creating network ' + NETWORK)
        run(dockerCommand + ' network create --driver=overlay --attachable ' + NETWORK)
    } else {
        println 'Found network ' + NETWORK
    }
}

def doesNetworkExist(dockerCommand) {
    header('Checking for network: ' + NETWORK)
    def command = dockerCommand + ' network ls'
    def process = command.execute()
    def found = false;
    process.in.eachLine {
        line ->
           def alteredLine = line.replaceAll('\\s+', ':')
           def networkName = alteredLine.tokenize(':')[1]
           if(NETWORK.equals(networkName)) {
               found = true
           }
        }

    if(!found) {
        println 'Network ' + NETWORK + ' not found'
    }

    return found;
}

def EnvironmentVariablesToList() {
    return ENVIRONMENT.entrySet().stream().map({e -> e.getKey() + '=' + e.getValue()}).toList()
}

def run(command) {
    if(dryRun) {
        println('Environment Variables: '  + EnvironmentVariablesToList().toArray(new String[0]))
        println 'Dry Run Command: ' + command
        println(command)
    } else {
        println('Environment Variables: ' + EnvironmentVariablesToList().toArray(new String[0]))
        println 'Running: ' + command
        def proc = Runtime.getRuntime().exec(command, EnvironmentVariablesToList().toArray(new String[0]))
        proc.waitForProcessOutput(System.out, System.err)
    }
}

def header(message) {
    println ''
    println '# # # # # # # # # # # # # #'
    println ' ' + message
    println ''
}

def verifyEnvironmentVariablesExist() {
    def productRetrieveHost = System.getenv('PRODUCT_RETRIEVE_HOST')
    if(productRetrieveHost == null || productRetrieveHost.trim().isEmpty()) {
        println 'PRODUCT_RETRIEVE_HOST environment variable must be set'
        System.exit(1)
    }
    ENVIRONMENT.PRODUCT_RETRIEVE_HOST = productRetrieveHost
}
