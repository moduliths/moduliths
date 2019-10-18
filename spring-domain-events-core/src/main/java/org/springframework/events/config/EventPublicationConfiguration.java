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

import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.events.EventPublicationRegistry;
import org.springframework.events.support.CompletionRegisteringBeanPostProcessor;
import org.springframework.events.support.MapEventPublicationRegistry;
import org.springframework.events.support.PersistentApplicationEventMulticaster;

/**
 * @author Oliver Gierke
 */
@Configuration(proxyBeanMethods = false)
class EventPublicationConfiguration {

	@Bean
	PersistentApplicationEventMulticaster applicationEventMulticaster(ObjectProvider<EventPublicationRegistry> registry) {

		Supplier<EventPublicationRegistry> supplier = () -> registry
				.getIfAvailable(() -> new MapEventPublicationRegistry());

		return new PersistentApplicationEventMulticaster(supplier);
	}

	@Bean
	static CompletionRegisteringBeanPostProcessor bpp(ObjectFactory<EventPublicationRegistry> store) {
		return new CompletionRegisteringBeanPostProcessor(store);
	}
}
