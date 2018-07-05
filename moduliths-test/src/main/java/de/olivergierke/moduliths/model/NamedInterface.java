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
package de.olivergierke.moduliths.model;

import lombok.Getter;

import org.springframework.util.Assert;

import com.tngtech.archunit.core.domain.JavaClass;

/**
 * @author Oliver Gierke
 */
public class NamedInterface {

	private static final String UNNAMED_NAME = "<<UNNAMED>>";

	private final JavaPackage javaPackage;
	private final @Getter String name;

	private NamedInterface(JavaPackage javaPackage, String name) {

		Assert.notNull(javaPackage, "Package must not be null!");
		Assert.hasText(name, "Package name must not be null or empty!");

		this.javaPackage = javaPackage.toSingle();
		this.name = name;
	}

	static NamedInterface unnamed(JavaPackage javaPackage) {
		return new NamedInterface(javaPackage, UNNAMED_NAME);
	}

	public static NamedInterface of(JavaPackage javaPackage) {

		String name = javaPackage.getAnnotation(de.olivergierke.moduliths.NamedInterface.class) //
				.map(it -> it.value()) //
				.orElseThrow(() -> new IllegalArgumentException("Couldn't find NamedInterface annotation on package!"));

		return new NamedInterface(javaPackage, name);
	}

	public boolean isUnnamed() {
		return name.equals(UNNAMED_NAME);
	}

	public boolean contains(JavaClass type) {
		return javaPackage.contains(type);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("%s - %s", name, javaPackage.getName());
	}
}
