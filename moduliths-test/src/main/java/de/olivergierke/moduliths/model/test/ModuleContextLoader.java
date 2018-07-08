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
import de.olivergierke.moduliths.model.Modules;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.ApplicationContextInitializer;
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.boot.test.context.SpringBootContextLoader#getInitializers(org.springframework.test.context.MergedContextConfiguration, org.springframework.boot.SpringApplication)
	 */
	@Override
	protected List<ApplicationContextInitializer<?>> getInitializers(MergedContextConfiguration config,
			SpringApplication application) {

		List<ApplicationContextInitializer<?>> initializers = new ArrayList<>(super.getInitializers(config, application));

		initializers.add(applicationContext -> {

			ModuleTestExecution execution = ModuleTestExecution.of(config.getTestClass());

			logModules(execution);

			applicationContext.getBeanFactory().registerSingleton(ModuleTestExecution.class.getName(), execution);
		});

		return initializers;
	}

	private static void logModules(ModuleTestExecution execution) {

		Module module = execution.getModule();
		Modules modules = execution.getModules();
		String moduleName = module.getDisplayName();
		String bootstrapMode = execution.getBootstrapMode().name();

		String message = String.format("Bootstrapping @ModuleTest for %s in mode %s…", moduleName, bootstrapMode);

		LOG.info(message);
		LOG.info(getSeparator("=", message));

		Arrays.stream(module.toString(modules).split("\n")).forEach(LOG::info);

		List<Module> dependencies = execution.getDependencies();

		if (!dependencies.isEmpty()) {

			LOG.info(getSeparator("=", message));
			LOG.info("Included dependencies:");
			LOG.info(getSeparator("=", message));

			dependencies.stream() //
					.map(it -> it.toString(modules)) //
					.forEach(it -> {
						Arrays.stream(it.split("\n")).forEach(LOG::info);
					});

			LOG.info(getSeparator("=", message));
		}
	}

	private static String getSeparator(String character, String reference) {
		return String.join("", Collections.nCopies(reference.length(), character));
	}
}
