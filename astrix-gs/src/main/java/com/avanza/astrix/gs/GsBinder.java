/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.gs;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;

import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.gigaspaces.internal.client.spaceproxy.IDirectSpaceProxy;
import com.gigaspaces.internal.server.space.SpaceImpl;
import com.j_spaces.core.JSpaceContainerImpl;

/**
 * 
 * @author Elias Lindholm
 *
 */
public class GsBinder implements AstrixConfigAware {
	
	public static final String SPACE_NAME_PROPERTY = "spaceName";
	public static final String SPACE_URL_PROPERTY = "spaceUrl";
	private static final String SPACE_REQUIRES_AUTHENTICATION = "isSecured";
	public static final String START_TIME = "startTime";

	private static final Pattern SPACE_URL_PATTERN = Pattern.compile("jini://.*?/.*?/(.*)?[?](.*)");
	private DynamicConfig config;
	
	public GigaSpace getEmbeddedSpace(ApplicationContext applicationContext) {
		String optionalGigaSpaceBeanName = AstrixSettings.GIGA_SPACE_BEAN_NAME.getFrom(config).get();
		if (optionalGigaSpaceBeanName  != null) {
			return applicationContext.getBean(optionalGigaSpaceBeanName, GigaSpace.class);
		}
		return findEmbeddedSpace(applicationContext);
	}

	private GigaSpace findEmbeddedSpace(ApplicationContext applicationContext) {
		GigaSpace result = null;
		for (GigaSpace gigaSpace : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, GigaSpace.class).values()) {
			if (isEmbedded(gigaSpace)) {
				if (result != null) {
					throw new IllegalStateException("Multiple embedded spaces defined in applicationContext");
				} else {
					result = gigaSpace;
				}
			}
		}
		if (result == null) {
			throw new IllegalStateException("Failed to find an embedded space in applicationContext");
		}
		return result;
	}

	private boolean isEmbedded(GigaSpace gigaSpace) {
		try {
			return gigaSpace.getSpace().isEmbedded();
		} catch (Exception e) {
			// Clearly not an embedded space since they are not stateful
			return false;
		}
	}

	static String getSpaceName(ServiceProperties serviceProperties) {
		return serviceProperties.getProperty(SPACE_NAME_PROPERTY);
	}

	static String getSpaceUrl(ServiceProperties serviceProperties) {
		return serviceProperties.getProperty(SPACE_URL_PROPERTY);
	}

	static boolean isAuthenticationRequired(ServiceProperties serviceProperties) {
		return Boolean.parseBoolean(serviceProperties.getProperty(SPACE_REQUIRES_AUTHENTICATION));
	}

	public ServiceProperties createProperties(GigaSpace space) {
		ServiceProperties result = new ServiceProperties();
//		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, space.getSpace().getName());
		result.setProperty(SPACE_URL_PROPERTY, new SpaceUrlBuilder(space).buildSpaceUrl());
		result.setProperty(SPACE_REQUIRES_AUTHENTICATION, Boolean.toString(space.getSpace().isSecured()));
		getInstanceStartTime(space).ifPresent(t -> result.setProperty(START_TIME, t.toString()));
		return result;
	}

	private Optional<Long> getInstanceStartTime(GigaSpace space) {
		return Optional.ofNullable(space.getSpace().getDirectProxy())
				.map(IDirectSpaceProxy::getSpaceImplIfEmbedded)
				.map(SpaceImpl::getContainer)
				.map(JSpaceContainerImpl::getStartTime);
	}

	public ServiceProperties createServiceProperties(String spaceUrl) {
		Matcher spaceUrlMatcher = SPACE_URL_PATTERN.matcher(spaceUrl);
		if (!spaceUrlMatcher.find()) {
			throw new IllegalArgumentException("Invalid spaceUrl: " + spaceUrl);
		}
		String spaceName = spaceUrlMatcher.group(1);
		ServiceProperties result = new ServiceProperties();
//		result.setApi(GigaSpace.class);
		result.setProperty(SPACE_NAME_PROPERTY, spaceName);
		result.setProperty(SPACE_URL_PROPERTY, spaceUrl);
		result.setQualifier(spaceName);
		return result;
	}
	
	private static class SpaceUrlBuilder {
		private String locators;
		private String groups;
		private String spaceName;
		private String versioned;
		
		public SpaceUrlBuilder(GigaSpace space) {
			var finderURL = space.getSpace().getFinderURL();
			this.locators = finderURL.getProperty("locators");
			this.groups = finderURL.getProperty("groups");
			this.versioned = Boolean.toString(space.getSpace().isOptimisticLockingEnabled());
			this.spaceName = finderURL.getSpaceName();
		}

		public String buildSpaceUrl() {
			StringBuilder result = new StringBuilder();
			result.append("jini://*/*/");
			result.append(spaceName);
			result.append("?");
			if (locators != null) {
				result.append("locators=");
				result.append(locators);
			} else {
				result.append("groups=");
				result.append(groups);
			}
			result.append("&versioned=").append(this.versioned);
			return result.toString();
		}
		
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}

}
