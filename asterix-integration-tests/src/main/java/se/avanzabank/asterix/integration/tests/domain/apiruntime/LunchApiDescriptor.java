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
package se.avanzabank.asterix.integration.tests.domain.apiruntime;

import se.avanzabank.asterix.integration.tests.domain.api.LunchService;
import se.avanzabank.asterix.provider.core.AstrixServiceBusApi;
import se.avanzabank.asterix.provider.remoting.AstrixRemoteApiDescriptor;
import se.avanzabank.asterix.provider.versioning.AstrixVersioned;


// API:t är versionshanterat
@AstrixVersioned(
	apiMigrations = {
		LunchApiV1Migration.class
	},	
	version = 2,
	objectMapperConfigurer = LunchApiObjectMapperConfigurer.class
)
// Tjänsten publiceras och kan slås upp via tjänstebussen
@AstrixServiceBusApi
// Tjänsten exponerar en gs-remoting-tjänst
@AstrixRemoteApiDescriptor (
	exportedApis = {
		LunchService.class
	}
)
public class LunchApiDescriptor {
	
	/*
	 * TODO: how to export pub-sub/services? local-snapshots? Other not yet known aspects? 
	 * We need an extension point for new types of 'services'. Annotate each exported api
	 * with info about what type of api it is?
	 */
	
}


