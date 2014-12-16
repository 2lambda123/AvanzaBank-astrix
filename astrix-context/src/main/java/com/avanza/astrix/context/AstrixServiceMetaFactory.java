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

import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixServiceMetaFactory implements AstrixSettingsAware {

	private AstrixServiceComponents serviceComponents;
	private AstrixServiceLeaseManager leaseManager;
	private AstrixSettingsReader settings;

	public <T> AstrixServiceFactory<T> createServiceFactory(ServiceVersioningContext versioningContext, AstrixServiceLookup serviceLookup, Class<T> serviceApi) {
		return new AstrixServiceFactory<>(versioningContext, serviceApi, serviceLookup, serviceComponents, leaseManager, settings);
	}
	
	public Class<?> loadInterfaceIfExists(String interfaceName) {
		try {
			Class<?> c = Class.forName(interfaceName);
			if (c.isInterface()) {
				return c;
			}
		} catch (ClassNotFoundException e) {
			// fall through and return null
		}
		return null;
	}

	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setLeaseManager(AstrixServiceLeaseManager leaseManager) {
		this.leaseManager = leaseManager;
	}
	
	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}


}
