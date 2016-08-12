# spring-cloud-deployer-openshift

A Spring Cloud Deployer SPI implementation which extends 
[Spring Cloud Deployer Kubernetes](https://github.com/spring-cloud/spring-cloud-deployer-kubernetes)
and adds functionality afforded by OpenShift 3 as the deployment environment.

## Supported URI's/Resources

The following app URI's/Resources are supported:

* `docker:` resources as supported by the Kubernetes deployer
* `maven:` resources as supported by the Local deployer

### Docker resource

The OpenShift deployer aims to implement most of the functionality available in the Kubernetes deployer.
This currently includes the ability to deploy/launch `docker:` registered apps/tasks.

Some Kubernetes functionality is implemented in more OpenShift specific ways.
For example, the `LoadBalancer` option is implemented as a 
[`Route`](https://docs.openshift.org/latest/dev_guide/routes.html) resource in OpenShift.

An example stream definition can be deployed as follows:

```
dataflow:>app register --type source --name time --uri docker:springcloudstream/time-source-kafka:latest
Successfully registered application 'source:time'
dataflow:>app register --type sink --name log --uri docker:springcloudstream/log-sink-kafka:latest
Successfully registered application 'sink:log'

dataflow:>stream create --name timelog --definition "time | log"
Created new stream 'timelog'
dataflow:>stream deploy timelog
Deployed stream 'timelog'
```

### Maven resource

Apps or tasks registered with the `maven:` URI are also supported by the OpenShift deployer.
An OpenShift [BuildConfig/Build](https://docs.openshift.org/latest/dev_guide/builds.html) is triggered when
a Maven resource is deployed and there is no existing successful build for the same artifact.
The Build uses the [Docker strategy](https://docs.openshift.org/latest/dev_guide/builds.html#docker-strategy-options) 
and various strategies to determine which input source to use:

* If a remote Git URI is specified when creating the stream/task definition using the `spring.cloud.deployer.openshift.build.git.uri` 
property, this repository will be used and has the highest precedence.
* If `src/main/docker/Dockerfile` is detected in the Maven artifact Jar, 
then it is assumed that the `Dockerfile` will exist in that location in a remote Git repository. 
In that case, the [Git repository source](https://docs.openshift.org/latest/dev_guide/builds.html#source-code) 
is used in conjunction with the Docker build strategy. The remote Git URI and ref are extracted from the 
`<scm><connection></connection></scm>` and `<scm><tag></tag></scm>` tags in the `pom.xml` of the Maven Jar artifact. 
For example, if the `<scm><connection>` value was `scm:git:git@github.com:spring-cloud/spring-cloud-dataflow.git`, 
then the remote Git URI would be parsed as `ssh://git@github.com:spring-cloud/spring-cloud-dataflow.git`. 
In short, the `Dockerfile` from the remote Git repository for the app being deployed will be used as the source for the image build. 
Of course, you can include and customise whatever and however you like in this `Dockerfile`.
* The other strategy uses the contents of a `Dockerfile` located in one of three locations as the 
[Dockerfile source](https://docs.openshift.org/latest/dev_guide/builds.html#dockerfile-source):
  * A file system location of a Dockerfile indicated by the `spring.cloud.deployer.openshift.deployment.dockerfile` deployment property. 
  E.g. `--properties "spring.cloud.deployer.openshift.deployment.dockerfile=/tmp/deployer/Dockerfile"`. 
  The *contents* of this file will be used as the source input for the build.
  * The inline Dockerfile content as provided in the `spring.cloud.deployer.openshift.deployment.dockerfile` deployment property.
  E.g. `--properties "spring.cloud.deployer.openshift.deployment.dockerfile=FROM java:8\n RUN wget ..."`
  * The default `Dockerfile` provided by the OpenShift deployer. Located in `src/main/resources`.

An example stream definition can be deployed as follows:

```
dataflow:>app register --type source --name http --uri maven://org.springframework.cloud.stream.app:http-source-kafka:1.0.0.BUILD-SNAPSHOT
Successfully registered application 'source:time'
dataflow:>app register --type sink --name log --uri maven://org.springframework.cloud.stream.app:log-sink-kafka:1.0.0.BUILD-SNAPSHOT
Successfully registered application 'sink:log'

dataflow:>stream create --name httplog --definition "http | log"
Created new stream 'httplog'
dataflow:>stream deploy httplog
Deployed stream 'httplog'
```

## Deployer properties

Global deployer settings

`spring.cloud.deployer.openshift.forceBuild` (default: `false`)

Force a build to be triggered for every app deployment regardless of previous build status.

`spring.cloud.deployer.openshift.defaultRoutingSubdomain` (default: `router.default.svc.cluster.local`)

The default router subdomain to use for Route's. See [here](https://docs.openshift.org/latest/install_config/install/deploy_router.html#customizing-the-default-routing-subdomain)
for more.

`spring.cloud.deployer.openshift.build.git.secret`

The source secret to be used if the remote Git repository requires authentication.

`spring.cloud.deployer.openshift.image.tag`

The default image tag to associate with Build's and DeploymentConfig's

## Definition properties

Properties defined on the stream/task definition

`spring.cloud.deployer.openshift.build.git.uri`

A remote Git URI containing the `Dockerfile` to use as the input source.

`spring.cloud.deployer.openshift.build.git.ref`

The Git branch/tag to use with the remote Git repository.

`spring.cloud.deployer.openshift.build.git.dockerfile`

The path where the Dockerfile is expected in the Git repository.

`spring.cloud.deployer.openshift.build.git.secret`

The source secret to be used if the remote Git repository requires authentication.

## Deployment properties

Properties defined on deployment of a defined stream/task

`spring.cloud.deployer.openshift.forceBuild`

Force an specific application to always trigger a new Build regardless of the previous build status.

`spring.cloud.deployer.openshift.deployment.service.account`

A [Service Account](https://docs.openshift.org/latest/dev_guide/service_accounts.html) that should be associated
with the application container.

`spring.cloud.deployer.openshift.image.tag`

The default image tag to associate with Build's and DeploymentConfig's.

`spring.cloud.deployer.openshift.deployment.dockerfile`

An absolute path on the file system to a Dockerfile or an inline Dockerfile definition.

`spring.cloud.deployer.openshift.deployment.nodeSelector`

A comma separated list of labels which indicate where to schedule pods.
Labels are in the format `region: primary,role: node`.

`spring.cloud.deployer.openshift.deployment.route.host`

The `host` value to use for the Route resource.

`spring.cloud.deployer.kubernetes.createLoadBalancer` / `spring.cloud.deployer.openshift.createRoute`

Boolean flag indicating whether a Route resource should be created for an application.

`spring.cloud.deployer.openshift.deployment.hostpath.volume`

A comma separated list of volume mounts/volumes to asssociate with an app deployment.
Mappings are in the following format:

`spring.cloud.deployer.openshift.deployment.hostpath.volume=volumeName:mountPath:hostPath`

where `volumeName` is the name of the volume (both volumeMount and volume), `mountPath` is the path mounted
inside the container and `hostPath` is the path mounted from the node.

*There is currently only support for the `hostPath` volume plugin.*
Please see [here](https://docs.openshift.org/latest/admin_guide/manage_scc.html#use-the-hostpath-volume-plugin) for
more information.

## Running the integration tests

The integration tests require a running OpenShift 3 instance.
Specify the connection details to your running OpenShift instance as System properties/environment variables before running the tests.
E.g.

```
$ export KUBERNETES_NAMESPACE=test
$ ./mvnw -Dopenshift.url=https://172.28.128.4:8443 \
  -Dkubernetes.master=https://172.28.128.4:8443 \
  -Dkubernetes.trust.certificates=true \
  -Dkubernetes.auth.basic.username=admin \
  -Dkubernetes.auth.basic.password=admin \
  test
```

## Further Reading

Please see the following posts for more information:

* [Spring Cloud Deployer OpenShift](http://blog.switchbit.io/spring-cloud-deployer-openshift)
* [SCDF OpenShift: Deploying Maven artifacts with custom Dockerfile](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile)

