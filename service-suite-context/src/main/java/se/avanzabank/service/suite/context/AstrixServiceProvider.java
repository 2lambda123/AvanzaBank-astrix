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
package se.avanzabank.service.suite.context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AstrixServiceProvider {
	
	// TODO: rename to AstrixApiProvider and reserve the term service-provider for server side use?
	
	private ConcurrentMap<Class<?>, AstrixServiceFactory<?>> serviceFactoryByProvidedService = new ConcurrentHashMap<>();
	private Class<?> descriptorHolder;
	
	public AstrixServiceProvider(List<AstrixServiceFactory<?>> factories, Class<?> descriptorHolder) {
		this.descriptorHolder = descriptorHolder;
		for (AstrixServiceFactory<?> factory : factories) {
			AstrixServiceFactory<?> previous = this.serviceFactoryByProvidedService.putIfAbsent(factory.getServiceType(), factory);
			if (previous != null) {
				throw new IllegalArgumentException("Multiple service factories found on: " + descriptorHolder);
			}
		}
	}
	
	public Class<?> getDescriptorHolder() {
		return descriptorHolder;
	}

	public Collection<Class<?>> providedServices() {
		return this.serviceFactoryByProvidedService.keySet();
	}
	
	public <T> AstrixServiceFactory<T> getServiceFactory(Class<T> type) {
		return (AstrixServiceFactory<T>) this.serviceFactoryByProvidedService.get(type);
	}
	
}
