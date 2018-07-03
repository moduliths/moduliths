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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;

/**
 * Bootstraps the module containing the package of the test class annotated with {@link ModuleTest}. Will apply the
 * following modifications to the Spring Boot configuration:
 * <ul>
 * <li>Restricts the component scanning to the module's package.
 * <li>
 * <li>Sets the module's package as the only auto-configuration and entity scan package.
 * <li>
 * </ul>
 * 
 * @author Oliver Gierke
 */
@Retention(RetentionPolicy.RUNTIME)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@TypeExcludeFilters(ModuleTypeExcludeFilter.class)
@ContextConfiguration(loader = ModuleContextLoader.class)
public @interface ModuleTest {
}
