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
package com.avanza.astrix.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.Ordered;

import com.avanza.astrix.beans.registry.AstrixServiceRegistryPlugin;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixContextImpl;
import com.avanza.astrix.serviceunit.AstrixApplicationDescriptor;
import com.avanza.astrix.serviceunit.AstrixServiceExporter;

/**
 * 
 * @author Elias Lindholm (elilin)
 */
public class AstrixFrameworkBootstrapper implements BeanFactoryPostProcessor, ApplicationContextAware, ApplicationListener<ApplicationContextEvent>, Ordered {
	
	/*
	 * IMPLEMENTATION NOTE - Astrix startup
	 * 
	 * The startup procedure goes as follows:
	 * 
	 * 1. BeanFactoryPostProcessor (BFPP)
	 *  - The BFPP will register an instance for each consumedAstrixBean in the BeanFactory
	 *  - The BFPP will also register an instance of AstrixSpingContext to act as bridge
	 *    between spring and astrix.
	 *    
	 * 2. BeanPostProcessor (BPP)
	 *  - The BPP will investigate each spring-bean in the current application and search
	 *    for @AstrixServiceExport annotated classes and register those with the AstrixServiceExporter
	 *    
	 * 3. ApplicationListener<ContexRefreshedEvent>
	 *  - After each spring-bean have bean created and fully initialized this class will receive a call-back
	 *    and start exporting services using the AstrixServiceExporter.
	 *    
	 * 
	 * Note that this class (AstrixFrameworkBean) is the only class in the framework that will recieve spring-lifecylce events.
	 * 
	 */
	
	private List<Class<?>> consumedAstrixBeans = new ArrayList<>();
	private String subsystem;
	private Map<String, String> settings = new HashMap<>();
	private AstrixApplicationDescriptor applicationDescriptor;
	private AstrixContextImpl astrixContext;
	private volatile boolean serviceExporterStarted = false;
	private ApplicationContext applicationContext;
	
	public AstrixFrameworkBootstrapper() {
	}
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		astrixContext = createAsterixContext(getDynamicConfig(applicationContext));
		astrixContext.getInstance(AstrixSpringContext.class).setApplicationContext(applicationContext);
		for (Class<?> consumedAstrixBean : this.consumedAstrixBeans) {
			beanFactory.registerSingleton(consumedAstrixBean.getName(), astrixContext.getBean(consumedAstrixBean));
		}
		beanFactory.registerSingleton(AstrixSpringContext.class.getName(), astrixContext.getInstance(AstrixSpringContext.class));
		beanFactory.registerSingleton(AstrixContext.class.getName(), astrixContext);
		beanFactory.addBeanPostProcessor(astrixContext.getInstance(AstrixBeanPostProcessor.class));
	}

	public void setConsumedAstrixBeans(List<Class<? extends Object>> consumedAstrixBeans) {
		this.consumedAstrixBeans = consumedAstrixBeans;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings.putAll(settings);
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}
	
	@PreDestroy
	public void destroy() {
		this.astrixContext.destroy();
	}
	
	/**
	 * If a service descriptor is provided, then the service exporting part of the framework
	 * will be loaded with all required components for the given serviceDescriptor.
	 * 
	 * @param serviceDescriptor
	 * @deprecated - replaced by {@link AstrixFrameworkBootstrapper#setApplicationDescriptor(Class)}
	 */
	@Deprecated
	public void setServiceDescriptor(Class<?> serviceDescriptorHolder) {
		this.applicationDescriptor = AstrixApplicationDescriptor.create(serviceDescriptorHolder);
	}
	
	/**
	 * If an application descriptor is provided, then the service exporting part of the framework
	 * will be loaded with all required components to provide the services defined in
	 * the api's provided by the given applicatinDescriptor.
	 * 
	 * @param serviceDescriptor
	 */
	public void setApplicationDescriptor(AstrixApplicationDescriptor applicationDescriptor) {
		this.applicationDescriptor = applicationDescriptor;
	}
	
	
	
	/**
	 * All services consumed by the current application. Each type will be created and available
	 * for autowiring in the current applicationContext.
	 * 
	 * Implementation note: This is only application defined usages of Astrix beans. Any Astrix-beans
	 * used internally by the service-framework will not be included in this set. 
	 * 
	 * @return
	 */
	public List<Class<?>> getConsumedAstrixBeans() {
		return consumedAstrixBeans;
	}
	
	public void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	private DynamicConfig getDynamicConfig(ApplicationContext applicationContext) {
		Collection<DynamicConfig> dynamicConfigs = applicationContext.getBeansOfType(DynamicConfig.class).values();
		if (dynamicConfigs.isEmpty()) {
			return null;
		}
		if (dynamicConfigs.size() == 1) {
			return dynamicConfigs.iterator().next();
		}
		throw new IllegalArgumentException("Multiple DynamicConfig instances found in ApplicationContext");
	}
	
	private AstrixContextImpl createAsterixContext(DynamicConfig optionalConfig) {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setSettings(this.settings);
		if (optionalConfig != null) {
			configurer.setConfig(optionalConfig);
		}
		if (this.subsystem != null) {
			configurer.setSubsystem(this.subsystem);
		}
		AstrixContextImpl astrixContext = (AstrixContextImpl) configurer.configure();
		return astrixContext;
	}

	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		if (event instanceof ContextRefreshedEvent && !serviceExporterStarted) {
			// Application initialization complete. Export asterix-services.
			exportAllProvidedServices();
			serviceExporterStarted = true;
		} else if (event instanceof ContextClosedEvent || event instanceof ContextStoppedEvent) {
			/*
			 * What's the difference between the "stopped" and "closed" event? In our embedded
			 * integration tests we only receive ContextClosedEvent
			 */
			destroyAstrixContext();
		}
	}

	private void destroyAstrixContext() {
		this.astrixContext.destroy();
	}

	private void exportAllProvidedServices() {
		if (applicationDescriptor == null) {
			return; // current application exports no services
		}
		AstrixServiceExporter serviceExporter = astrixContext.getInstance(AstrixServiceExporter.class);
		serviceExporter.setServiceDescriptor(applicationDescriptor); // TODO This is a hack. Avoid setting serviceDescriptor explicitly here
		serviceExporter.exportProvidedServices();
		astrixContext.getPlugin(AstrixServiceRegistryPlugin.class).startPublishServices();
	}

	public void setConsumedAstrixBeans(Class<?>... consumedAstrixBeans) {
		setConsumedAstrixBeans(Arrays.asList(consumedAstrixBeans));
	}
	
	@Override
	public int getOrder() {
		// Run this class as late as possible
		return Ordered.LOWEST_PRECEDENCE;
	}
	
}