/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.events.config;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;

/**
 * An {@link ObjectFactory} that allows to define a fallback {@link Supplier} to be used in case the lookup on the
 * original delegate fails.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class DefaultingObjectFactory<T> implements ObjectFactory<T> {

	private final ObjectFactory<T> delegate;
	private final Supplier<T> fallback;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.ObjectFactory#getObject()
	 */
	@Override
	public T getObject() throws BeansException {

		try {
			return delegate.getObject();
		} catch (NoSuchBeanDefinitionException o_O) {
			return fallback.get();
		}
	}
}
