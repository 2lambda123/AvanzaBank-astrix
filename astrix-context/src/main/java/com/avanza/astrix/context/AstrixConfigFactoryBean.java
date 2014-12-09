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
package com.avanza.astrix.context;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixConfigFactoryBean<T> implements AstrixFactoryBeanPlugin<T> {
	
	private String  entryName;
	private AstrixApiDescriptor descriptor;
	private Class<T> api;
	private AstrixSettingsReader settings;
	private AstrixServiceComponents serviceComponents;

	public AstrixConfigFactoryBean(String entryName, AstrixApiDescriptor descriptor, Class<T> beanType, AstrixSettingsReader settings) {
		this.entryName = entryName;
		this.descriptor = descriptor;
		this.api = beanType;
		this.settings = settings;
	}

	@Override
	public T create(String optionalQualifier) {
		String serviceUri = lookup();
		String component = serviceUri.substring(0, serviceUri.indexOf(":"));
		String servicePropertiesUri = serviceUri.substring(serviceUri.indexOf(":") + 1);
		AstrixServiceComponent serviceComponent = getServiceComponent(component);
		AstrixServiceProperties serviceProperties = serviceComponent.createServiceProperties(servicePropertiesUri);
		return serviceComponent.createService(descriptor, api, serviceProperties);
	}

	private AstrixServiceComponent getServiceComponent(String componentName) {
		return serviceComponents.getComponent(componentName);
	}

	private String lookup() {
		String result = this.settings.getString(entryName);
		if (result == null) {
			throw new IllegalStateException("Config entry not defined: " + this.entryName + ". Config: " + this.settings);
		}
		return result;
	}
	
	@Override
	public Class<T> getBeanType() {
		return this.api;
	}

	@AstrixInject
	public void setServiceComponenets(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}

}
