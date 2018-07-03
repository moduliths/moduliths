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
package de.olivergierke.moduliths.model.test;

import de.olivergierke.moduliths.model.Module;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Dedicated {@link ContextLoader} implementation to reconfigure both {@link AutoConfigurationPackages} and
 * {@link EntityScanPackages} to both only consider the package of the current test class.
 * 
 * @author Oliver Gierke
 */
@Slf4j
class ModuleContextLoader extends SpringBootContextLoader {

	private static String getSeparator(String character, String reference) {
		return String.join("", Collections.nCopies(reference.length(), character));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.boot.test.context.SpringBootContextLoader#getInitializers(org.springframework.test.context.MergedContextConfiguration, org.springframework.boot.SpringApplication)
	 */
	@Override
	protected List<ApplicationContextInitializer<?>> getInitializers(MergedContextConfiguration config,
			SpringApplication application) {

		ModuleTestClass testClass = new ModuleTestClass(config.getTestClass());

		Module module = testClass.getModule();
		String moduleName = module.getDisplayName();
		String bootstrapMode = testClass.getBootstrapMode().name();

		String message = String.format("Bootstrapping @ModuleTest for %s in mode %s…", moduleName, bootstrapMode);

		LOG.info(message);
		LOG.info(getSeparator("=", message));

		Arrays.stream(module.toString().split("\n")).forEach(LOG::info);

		List<Module> dependencies = testClass.getDependencies();

		if (!dependencies.isEmpty()) {

			LOG.info(getSeparator("=", message));
			LOG.info("Included dependencies:");
			LOG.info(getSeparator("=", message));

			dependencies.stream() //
					.map(Module::toString) //
					.forEach(it -> {
						Arrays.stream(it.split("\n")).forEach(LOG::info);
					});

			LOG.info(getSeparator("=", message));
		}

		List<ApplicationContextInitializer<?>> initializers = super.getInitializers(config, application);

		initializers.add(new ApplicationContextInitializer<ConfigurableApplicationContext>() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.context.ApplicationContextInitializer#initialize(org.springframework.context.ConfigurableApplicationContext)
			 */
			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {

				List<String> basePackages = testClass.getBasePackages().collect(Collectors.toList());

				applicationContext.getBeanFactory().registerSingleton(
						"modulePackageToAutoConfigAndEntityScanPackagePostProcessor",
						new BeanFactoryPostProcessorImplementation(basePackages));
			}
		});

		return initializers;
	}

	/**
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private static class BeanFactoryPostProcessorImplementation implements BeanFactoryPostProcessor {

		private final List<String> packageNames;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

			setPackageOnBeanNamed(beanFactory, AutoConfigurationPackages.class.getName());
			setPackageOnBeanNamed(beanFactory, EntityScanPackages.class.getName());
		}

		private void setPackageOnBeanNamed(ConfigurableListableBeanFactory beanFactory, String beanName) {

			if (beanFactory.containsBeanDefinition(beanName)) {

				BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
				definition.getConstructorArgumentValues().addIndexedArgumentValue(0, packageNames);
			}
		}
	}
}
