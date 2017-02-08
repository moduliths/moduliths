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
package org.springframework.events.jpa;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.events.EventSerializer;
import org.springframework.events.config.EventPublicationConfigurationExtension;

/**
 * @author Oliver Gierke
 */
@Configuration
@EnableJpaRepositories
@RequiredArgsConstructor
public class JpaEventPublicationConfiguration implements EventPublicationConfigurationExtension {

	private final JpaEventPublicationRepository repository;
	private final ObjectFactory<EventSerializer> serializer;

	@Bean
	public JpaEventPublicationRegistry jpaEventPublicationRegistry() {
		return new JpaEventPublicationRegistry(repository, serializer.getObject());
	}
}
