# Kubernetes Deployment
## Deployment
### The Tool
To work with a K8s cluster you need to install Kubectl, a tool for controlling your K8s deployment.
[Kubectl installation guide](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

The `docker` command is the Docker Swarm equivalent to `kubectl`.

By default the deploy-k8s.groovy deployment script will deploy to a local
[Minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/) instance. To deploy to a remote
Kubernetes cluster, you will need to add the the Docker registry environment variable `K8S_DOCKER_REGISTRY`. You
will also need to change `kubectl`"s context to the remote one with `kubectl config use-context <remote-context>`

### Deploying
The deployment manifest file contains everything to deploy our application to the Kubernetes cluster. To deploy use:
`kubectl apply -f search-deployment.yml`

To remove a deployment and all related pods and containers use:
`kubectl delete -f search-deployment.yml`

Note: The ConfigMap will not be deleted automatically because it was added to the cluster manually.
#### Optional:
Install MiniKube, a single node Kubernetes cluster that runs locally in a VM or the host machine. Deploys a lightweight single-node k8s cluster.
Note: Run `eval $(minikube docker-env)` to point to Minikube"s docker daemon. You need to do this when you build images
for local deployment.

When starting up Minikube, make sure your vm-driver is the one bundled with
VirtualBox. Dnsmasq doesn"t play nice with the standalone hyperkit.


## Command Cheat Sheet
- `kubectl cluster-info`: View Cluster info
- `kubectl get` - List resources
    - `kubectl get nodes`: Lists available Nodes in the cluster
    - `kubectl get pods`: Lists Pods in your cluster
    - `kubectl get deployments`: List deployments in your cluster
- `kubectl describe` - Show detailed information about a resource
- `kubectl logs` - Print the logs from a container in a pod
- `kubectl exec` - Execute a command on a container in a pod
- `kubectl exec -ti <pod_name> sh`: Starts a bash session in the pod"s container. If there"s more than one container add `--container <container_name>`
- `kubectl exec <pod_name> env`: Lists environment variables

The [Docker to K8s Migration Guide](https://github.com/connexta/grayskull/blob/master/kubernetes/Docker_To_Kubernetes_Guide.md#kubernetes-2)
has a lot of good info
