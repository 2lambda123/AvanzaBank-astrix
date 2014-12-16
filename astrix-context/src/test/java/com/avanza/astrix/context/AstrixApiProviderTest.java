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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.library.AstrixExport;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.provider.versioning.AstrixVersioned;
import com.avanza.astrix.provider.versioning.NonVersioned;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;


public class AstrixApiProviderTest {
	
	@Test
	public void apiWithOneLibrary() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class);
		assertEquals("foo", ping.ping("foo"));
	}
	
	@Test(expected = IllegalAstrixApiProviderException.class)
	public void apiProviderNotProvidingDefinedLibrary() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(InvalidPingLibraryProvider.class);
		astrixConfigurer.configure();
	}
	
	@Test
	public void apiWithOneLibraryAndOneService() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, new PingServiceImpl());
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingServiceAndLibraryProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib pingLib = context.getBean(PingLib.class);
		assertEquals("foo", pingLib.ping("foo"));
		
		PingService pingService = context.getBean(PingService.class);
		assertEquals("bar", pingService.ping("bar"));
	}
	
	
	@Test
	public void versionedApi() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl(), 
																		ServiceVersioningContext.versionedService(1, DummyObjectSerializerConfigurer.class));
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(AstrixVersioningPlugin.class, new JavaSerializationVersioningPlugin());
		astrixConfigurer.registerApiProvider(VersionedPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		astrixConfigurer.enableVersioning(true);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService = context.getBean(PingService.class);
		assertEquals("bar", pingService.ping("bar"));
	}
	
	@Test
	public void apiWithVersionedAnNonVersionedService() throws Exception {
		String pingServiceUri = AstrixDirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl(), 
																		ServiceVersioningContext.versionedService(1, DummyObjectSerializerConfigurer.class));
		String internalPingServiceUri = AstrixDirectComponent.registerAndGetUri(InternalPingService.class, 
																		new InternalPingServiceImpl(), 
																		ServiceVersioningContext.nonVersioned());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(AstrixVersioningPlugin.class, new JavaSerializationVersioningPlugin());
		astrixConfigurer.registerApiProvider(PublicAndInternalPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		astrixConfigurer.set("internalPingServiceUri", internalPingServiceUri);
		astrixConfigurer.enableVersioning(true);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService = context.getBean(PingService.class);
		InternalPingService internalPingService = context.getBean(InternalPingService.class);
		assertEquals("foo", pingService.ping("foo"));
		assertEquals("bar", internalPingService.ping("bar"));
	}
	
	public interface PingLib {
		String ping(String msg);
	}
	
	public interface PingService {
		String ping(String msg);
	}

	public interface InternalPingService {
		String ping(String msg);
	}
	
	public static class PingLibImpl implements PingLib {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class PingServiceImpl implements PingService {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class InternalPingServiceImpl implements InternalPingService {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public interface PingLibraryApi {
		@Library
		PingLib pingLib();
	}

	public interface PingServiceApi {
		
		@AstrixConfigLookup("pingServiceUri")
		@Service
		PingService pingService();
	}

	@NonVersioned
	public interface InternalPingServiceApi {
		@AstrixConfigLookup("internalPingServiceUri")
		@Service
		InternalPingService internalPingService();
	}
	

	public interface PingServiceAndLibraryApi {
		@Library
		PingLib pingLib();
		
		@AstrixConfigLookup("pingServiceUri")
		@Service
		PingService pingService();
	}
	
	@AstrixApiProvider(PingLibraryApi.class)
	public static class PingLibraryProvider {
		@AstrixExport
		public PingLib myLib() {
			return new PingLibImpl();
		}
	}
	
	@AstrixApiProvider(PingServiceAndLibraryApi.class)
	public static class PingServiceAndLibraryProvider {
		@AstrixExport
		public PingLib myLib() {
			return new PingLibImpl();
		}
	}
	
	@AstrixApiProvider(PingLibraryApi.class)
	public static class InvalidPingLibraryProvider {
		// No export of PingLib
	}
	
	@AstrixVersioned(
		version = 1,
		objectSerializerConfigurer = DummyObjectSerializerConfigurer.class
	)
	@AstrixApiProvider(PingServiceApi.class)
	public static class VersionedPingServiceProvider {
	}
	
	@AstrixVersioned(
		version = 1,
		objectSerializerConfigurer = DummyObjectSerializerConfigurer.class
	)
	@AstrixApiProvider({PingServiceApi.class, InternalPingServiceApi.class})
	public static class PublicAndInternalPingServiceProvider {
	}
	
	public static class DummyObjectSerializerConfigurer implements AstrixObjectSerializerConfigurer {
	}

}
