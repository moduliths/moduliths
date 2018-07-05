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

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;

/**
 * @author Oliver Gierke
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JavaPackage implements DescribedIterable<JavaClass> {

	private static final String PACKAGE_INFO_NAME = "package-info";

	private final @Getter Classes classes;
	private final @Getter String name;

	public static JavaPackage forSingle(Classes classes, String name) {
		return new JavaPackage(classes.that(resideInAPackage(name)), name);
	}

	public static JavaPackage forNested(Classes classes, String name) {
		return new JavaPackage(classes.that(resideInAPackage(name.concat(".."))), name);
	}

	public JavaPackage toSingle() {
		return new JavaPackage(classes.that(resideInAnyPackage(name)), name);
	}

	public String getLocalName() {
		return name.substring(name.lastIndexOf(".") + 1);
	}

	public Collection<JavaPackage> getDirectSubPackages() {

		return classes.that(resideInAPackage(name.concat(".."))).stream() //
				.map(it -> it.getPackage()) //
				.filter(it -> !it.equals(name)) //
				.map(it -> extractDirectSubPackage(it)) //
				.distinct() //
				.map(it -> forNested(classes, it)) //
				.collect(Collectors.toSet());
	}

	/**
	 * Extract the direct sub-package name of the given candidate.
	 * 
	 * @param candidate
	 * @return
	 */
	private String extractDirectSubPackage(String candidate) {

		if (candidate.length() <= name.length()) {
			return candidate;
		}

		int subSubPackageIndex = candidate.indexOf('.', name.length() + 1);
		int endIndex = subSubPackageIndex == -1 ? candidate.length() : subSubPackageIndex;

		return candidate.substring(0, endIndex);
	}

	public Stream<JavaPackage> getSubPackagesAnnotatedWith(Class<? extends Annotation> annotation) {

		DescribedPredicate<JavaClass> predicate = resideInAPackage(name.concat(".."))
				.and(CanBeAnnotated.Predicates.annotatedWith(annotation));

		return classes.that(predicate).stream() //
				.map(JavaClass::getPackage).distinct() //
				.map(it -> forNested(classes, it));
	}

	public Classes that(DescribedPredicate<? super JavaClass> predicate) {
		return classes.that(predicate);
	}

	public boolean contains(JavaClass type) {
		return classes.contains(type);
	}

	public Stream<JavaClass> stream() {
		return classes.stream();
	}

	public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType) {

		return classes
				.that(JavaClass.Predicates.simpleName(PACKAGE_INFO_NAME)
						.and(CanBeAnnotated.Predicates.annotatedWith(annotationType))) //
				.toOptional() //
				.map(it -> it.getAnnotationOfType(annotationType));
	}

	/* 
	 * (non-Javadoc)
	 * @see com.tngtech.archunit.base.HasDescription#getDescription()
	 */
	@Override
	public String getDescription() {
		return classes.getDescription();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<JavaClass> iterator() {
		return classes.iterator();
	}
}
