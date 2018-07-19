/*
 * Copyright 2018 the original author or authors.
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
package de.olivergierke.moduliths.test;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * An unconditional auto-configuration registering an {@link ImportBeanDefinitionRegistrar} to customize both the entity
 * scan and auto-configuration packages to the packages defined by the {@link ModuleTestExecution} in the application
 * context.
 * 
 * @author Oliver Gierke
 */
@Configuration
@Import(ModuleTestAutoConfiguration.AutoConfigurationAndEntityScanPackageCustomizer.class)
class ModuleTestAutoConfiguration {

	private static final String AUTOCONFIG_PACKAGES = "org.springframework.boot.autoconfigure.AutoConfigurationPackages";
	private static final String ENTITY_SCAN_PACKAGE = "org.springframework.boot.autoconfigure.domain.EntityScanPackages";

	@Slf4j
	static class AutoConfigurationAndEntityScanPackageCustomizer implements ImportBeanDefinitionRegistrar {

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
		 */
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

			ModuleTestExecution execution = ((BeanFactory) registry).getBean(ModuleTestExecution.class);
			List<String> basePackages = execution.getBasePackages().collect(Collectors.toList());

			LOG.info("Re-configuring auto-configuration and entity scan packages to: {}.",
					StringUtils.collectionToDelimitedString(basePackages, ", "));

			setBasePackagesOn(registry, AUTOCONFIG_PACKAGES, "packages", basePackages);
			setBasePackagesOn(registry, ENTITY_SCAN_PACKAGE, "packageNames", basePackages);
		}

		private void setBasePackagesOn(BeanDefinitionRegistry registry, String beanName, String fieldName,
				List<String> packages) {

			if (!registry.containsBeanDefinition(beanName)) {
				return;
			}

			BeanDefinition definition = registry.getBeanDefinition(beanName);
			definition.getConstructorArgumentValues().addIndexedArgumentValue(0, packages);
		}
	}
}
