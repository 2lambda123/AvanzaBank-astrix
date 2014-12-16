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
package com.avanza.astrix.integration.tests.domain.apiruntime;

import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.provider.core.AstrixServiceProvider;
import com.avanza.astrix.provider.versioning.AstrixVersioned;


// The API is versioned.
@AstrixVersioned(
	version = 2,
	objectSerializerConfigurer = LunchApiObjectSerializerConfigurer.class
)
// The service is exported to the service-registry. Consumers queries the service-registry to bind to servers.
//@AstrixServiceProvider(LunchService.class)
public class LunchServiceProvider {
}


