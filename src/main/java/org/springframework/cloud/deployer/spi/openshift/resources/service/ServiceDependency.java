package org.springframework.cloud.deployer.spi.openshift.resources.service;

public class ServiceDependency {

	private String name;
	private final String kind = "Service";

	public ServiceDependency() {
	}

	public ServiceDependency(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getKind() {
		return kind;
	}
}
