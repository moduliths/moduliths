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

import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasName;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor(staticName = "of")
public class Classes implements DescribedIterable<JavaClass> {

	private final JavaClasses classes;

	/**
	 * Returns {@link Classes} that match the given {@link DescribedPredicate}.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return
	 */
	public Classes that(DescribedPredicate<? super JavaClass> predicate) {

		Assert.notNull(predicate, "Predicate must not be null!");

		return Classes.of(classes.that(predicate));
	}

	public Stream<JavaClass> stream() {
		return StreamSupport.stream(classes.spliterator(), false);
	}

	public boolean isEmpty() {
		return !classes.iterator().hasNext();
	}

	public Optional<JavaClass> toOptional() {
		return isEmpty() ? Optional.empty() : Optional.of(classes.iterator().next());
	}

	public boolean contains(JavaClass type) {
		return classes.that(new SameClass(type)).iterator().hasNext();
	}

	public boolean contains(String className) {
		return classes.that(HasName.Predicates.name(className)).iterator().hasNext();
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

	public static String format(JavaClass type) {
		return format(type, "");
	}

	public static String format(JavaClass type, String basePackage) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(basePackage, "Base package must not be null!");

		String prefix = type.getModifiers().contains(JavaModifier.PUBLIC) ? "+" : "o";
		String name = StringUtils.hasText(basePackage) //
				? type.getName().replace(basePackage, "…") //
				: type.getName();

		return String.format("%s %s", prefix, name);
	}

	private static class SameClass extends DescribedPredicate<JavaClass> {

		private final JavaClass reference;

		public SameClass(JavaClass reference) {
			super(" is the same class as ");
			this.reference = reference;
		}

		/* 
		 * (non-Javadoc)
		 * @see com.tngtech.archunit.base.DescribedPredicate#apply(java.lang.Object)
		 */
		@Override
		public boolean apply(JavaClass input) {
			return reference.getName().equals(input.getName());
		}
	}
}
