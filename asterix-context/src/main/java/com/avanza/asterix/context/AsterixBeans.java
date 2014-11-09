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
package com.avanza.asterix.context;

/**
 * Used by asterix-plugins that dependends on asterix-managed beans to 
 * get an instance of the given depenendencies at runtime. <p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixBeans {
	
	<T> T getBean(Class<T> beanType);

	<T> T getBean(Class<T> beanType, String qualifier);
	
}