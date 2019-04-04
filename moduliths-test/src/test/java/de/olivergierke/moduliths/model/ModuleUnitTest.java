/*
 * Copyright 2019 the original author or authors.
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
package de.olivergierke.moduliths.model;

import javax.sql.DataSource;

import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * Unit tests for {@link Module}.
 *
 * @author Oliver Drotbohm
 */
public class ModuleUnitTest {

	ClassFileImporter importer = new ClassFileImporter();

	@Test // #59
	public void considersExternalSpringBeans() {

		JavaClasses classes = importer.importPackages("com.acme.withatbean"); //
		JavaPackage javaPackage = JavaPackage.forNested(Classes.of(classes), "");

		Module module = new Module(javaPackage, false);

		module.getSpringBeans().contains(DataSource.class.getName());
	}
}
