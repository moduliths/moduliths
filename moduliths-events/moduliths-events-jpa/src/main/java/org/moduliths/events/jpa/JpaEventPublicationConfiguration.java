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
package org.moduliths.events.jpa;

import lombok.RequiredArgsConstructor;

import org.moduliths.events.EventSerializer;
import org.moduliths.events.config.EventPublicationConfigurationExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.util.ClassUtils;

/**
 * @author Oliver Gierke
 */
@Configuration(proxyBeanMethods = false)
@Import(JpaEventPublicationConfiguration.RepositoriesEnablingImportSelector.class)
@RequiredArgsConstructor
class JpaEventPublicationConfiguration implements EventPublicationConfigurationExtension {

	@Bean
	public JpaEventPublicationRegistry jpaEventPublicationRegistry(JpaEventPublicationRepository repository,
			EventSerializer serializer) {
		return new JpaEventPublicationRegistry(repository, serializer);
	}

	static class RepositoriesEnablingImportSelector implements ImportSelector {

		private static final boolean IN_SPRING_BOOT = ClassUtils.isPresent("org.springframework.boot.SpringApplication",
				RepositoriesEnablingImportSelector.class.getClassLoader());

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportSelector#selectImports(org.springframework.core.type.AnnotationMetadata)
		 */
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return IN_SPRING_BOOT ? new String[0] : new String[] { EnablingConfig.class.getName() };
		}

		@Configuration
		@EnableJpaRepositories
		static class EnablingConfig {}
	}
}
