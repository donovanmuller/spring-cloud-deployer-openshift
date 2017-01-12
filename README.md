# Spring Cloud Deployer for OpenShift [![Build Status](https://travis-ci.org/donovanmuller/spring-cloud-deployer-openshift.svg?branch=master)](https://travis-ci.org/donovanmuller/spring-cloud-deployer-openshift)

A Spring Cloud Deployer SPI implementation which extends 
[Spring Cloud Deployer Kubernetes](https://github.com/spring-cloud/spring-cloud-deployer-kubernetes)
and adds functionality afforded by [OpenShift](https://www.openshift.com/) as the deployment environment.

Please refer to the reference documentation on how to get started.

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

If you have a running OpenShift instance but do not want to execute the integration tests but rather just the unit tests.
Pass the `-Dopenshift.enabled=false` system property when running the tests:

```console
$ ./mvnw test -Dopenshift.enabled=false
```

## Further Reading

Please see the following posts for more information:

* [Spring Cloud Deployer OpenShift](http://blog.switchbit.io/spring-cloud-deployer-openshift)
* [SCDF OpenShift: Deploying Maven artifacts with custom Dockerfile](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile)

