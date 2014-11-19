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

import java.util.Objects;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixBeanKey {
	
	private Class<?> beanType;
	private String qualifier;

	private AstrixBeanKey(Class<?> beanType, String qualifier) {
		this.beanType = beanType;
		this.qualifier = qualifier;
	}
	
	public static AstrixBeanKey create(Class<?> beanType, String qualifier) {
		if (qualifier == null) {
			return new AstrixBeanKey(beanType, "-");
		} 
		return new AstrixBeanKey(beanType, qualifier);
	}
	
	public Class<?> getBeanType() {
		return beanType;
	}
	
	public String getQualifier() {
		if (qualifier.equals("-")) {
			return null;
		}
		return qualifier;
	}
	
	private String getBeanTypeName() {
		return beanType.getName();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getBeanTypeName(), qualifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstrixBeanKey other = (AstrixBeanKey) obj;
		return this.getBeanTypeName().equals(other.getBeanTypeName()) && this.qualifier.equals(other.qualifier);
	}
	
	@Override
	public String toString() {
		return getBeanTypeName() + "-" + qualifier;
	}
	
	

}