package org.springframework.cloud.deployer.spi.openshift.resources.service;

import java.util.ArrayList;
import java.util.List;


public class ServiceDependencies {

	private List<ServiceDependency> dependencies = new ArrayList<>();

	public ServiceDependencies() {
		// for Jackson
	}

	public ServiceDependencies(List<ServiceDependency> dependencies) {
		this.dependencies = dependencies;
	}

	public List<ServiceDependency> addServiceDependency(ServiceDependency dependency) {
		dependencies.add(dependency);
		return dependencies;
	}

	public List<ServiceDependency> getDependencies() {
		return dependencies;
	}
}
