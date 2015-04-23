/*
 * Copyright 2014-2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.astrix.serviceunit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.registry.ServiceState;
import com.avanza.astrix.beans.service.AstrixServiceComponent;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigAware;

public class ServiceRegistryExporter implements AstrixConfigAware {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceRegistryExporter.class);
	private final List<ServiceRegistryExportedService> exportedServices = new CopyOnWriteArrayList<>();
	private AstrixInjector injector;
	private DynamicConfig config;
	
	public <T> void addExportedService(ServiceBeanDefinition serviceBeanDefinition, AstrixServiceComponent serviceComponent) {
		String serviceState = "INACTIVE".equals(AstrixSettings.INITIAL_SERVICE_STATE.getFrom(config).get()) ? ServiceState.INACTIVE : ServiceState.ACTIVE;
		exportedServices.add(new ServiceRegistryExportedService(serviceComponent, serviceBeanDefinition, serviceState));
	}
	
	@AstrixInject
	public void setInjector(AstrixInjector injector) {
		this.injector = injector;
	}
	
	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
	public void startPublishServices() {
		if (exportedServices.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry.");
			return;
		}
		ServiceRegistryExporterWorker exporterWorker = getExporterWorker(); 
		for (ServiceRegistryExportedService serviceProperties : this.exportedServices) {
			exporterWorker.addServiceBuilder(serviceProperties);
		}
		exporterWorker.startServiceExporter();
	}
	
	public void setServiceState(String serviceState) {
		for (ServiceRegistryExportedService serviceProperties : this.exportedServices) {
			serviceProperties.setState(serviceState);
		}
		getExporterWorker().triggerServiceExport();
	}
	
	private ServiceRegistryExporterWorker getExporterWorker() {
		return injector.getBean(ServiceRegistryExporterWorker.class);
	}

}