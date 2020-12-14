/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.moduliths.model;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;

import lombok.experimental.UtilityClass;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates;

/**
 * @author Oliver Drotbohm
 */
@UtilityClass
class Types {

	@Nullable
	@SuppressWarnings("unchecked")
	<T> Class<T> loadIfPresent(String name) {

		ClassLoader loader = Types.class.getClassLoader();

		return ClassUtils.isPresent(name, loader) ? (Class<T>) ClassUtils.resolveClassName(name, loader) : null;
	}

	@Deprecated
	static class JDDDTypes {

		private static final String BASE_PACKAGE = "org.jddd";
		private static final String ANNOTATION_PACKAGE = BASE_PACKAGE + ".core.annotation";
		private static final String ENTITY_ANNOTATION = ANNOTATION_PACKAGE + ".Entity";
		private static final String ARCHUNIT_RULES = BASE_PACKAGE + ".archunit.JDddRules";

		static final String EVENT_ANNOTATION = BASE_PACKAGE + ".event.annotation.DomainEvent";
		static final String EVENT_TYPE = BASE_PACKAGE + ".event.types.DomainEvent";

		public static boolean isPresent() {
			return ClassUtils.isPresent(ENTITY_ANNOTATION, JDDDTypes.class.getClassLoader());
		}

		public static boolean areRulesPresent() {
			return ClassUtils.isPresent(ARCHUNIT_RULES, JDDDTypes.class.getClassLoader());
		}
	}

	static class JMoleculesTypes {

		private static final String BASE_PACKAGE = "org.jmolecules";
		private static final String ANNOTATION_PACKAGE = BASE_PACKAGE + ".ddd.annotation";
		private static final String ENTITY_ANNOTATION = ANNOTATION_PACKAGE + ".Entity";
		private static final String ARCHUNIT_RULES = BASE_PACKAGE + ".archunit.JDddRules";

		static final String EVENT_ANNOTATION = BASE_PACKAGE + ".event.annotation.DomainEvent";
		static final String EVENT_TYPE = BASE_PACKAGE + ".event.types.DomainEvent";

		public static boolean isPresent() {
			return ClassUtils.isPresent(ENTITY_ANNOTATION, JMoleculesTypes.class.getClassLoader());
		}

		public static boolean areRulesPresent() {
			return ClassUtils.isPresent(ARCHUNIT_RULES, JMoleculesTypes.class.getClassLoader());
		}
	}

	@UtilityClass
	static class JavaXTypes {

		private static final String BASE_PACKAGE = "javax";

		static final String AT_ENTITY = BASE_PACKAGE + ".persistence.Entity";
		static final String AT_INJECT = BASE_PACKAGE + ".inject.Inject";
		static final String AT_RESOURCE = BASE_PACKAGE + ".annotation.Resource";

		static DescribedPredicate<? super JavaClass> isJpaEntity() {
			return isAnnotatedWith(AT_ENTITY);
		}
	}

	@UtilityClass
	static class SpringTypes {

		private static final String BASE_PACKAGE = "org.springframework";

		static final String AT_AUTOWIRED = BASE_PACKAGE + ".beans.factory.annotation.Autowired";
		static final String AT_ASYNC = BASE_PACKAGE + ".scheduling.annotation.Async";
		static final String AT_BEAN = BASE_PACKAGE + ".context.annotation.Bean";
		static final String AT_COMPONENT = BASE_PACKAGE + ".stereotype.Component";
		static final String AT_CONFIGURATION = BASE_PACKAGE + ".context.annotation.Configuration";
		static final String AT_CONTROLLER = BASE_PACKAGE + ".stereotype.Controller";
		static final String AT_EVENT_LISTENER = BASE_PACKAGE + ".context.event.EventListener";
		static final String AT_REPOSITORY = BASE_PACKAGE + ".stereotype.Repository";
		static final String AT_SERVICE = BASE_PACKAGE + ".stereotype.Service";
		static final String AT_SPRING_BOOT_APPLICATION = BASE_PACKAGE + ".boot.autoconfigure.SpringBootApplication";
		static final String AT_TX_EVENT_LISTENER = BASE_PACKAGE + ".transaction.event.TransactionalEventListener";

		static DescribedPredicate<? super JavaClass> isConfiguration() {
			return isAnnotatedWith(AT_CONFIGURATION);
		}

		static DescribedPredicate<? super JavaClass> isComponent() {
			return isAnnotatedWith(AT_COMPONENT);
		}

		static boolean isAtBeanMethod(JavaMethod method) {
			return isAnnotatedWith(SpringTypes.AT_BEAN).apply(method);
		}
	}

	@UtilityClass
	static class SpringDataTypes {

		private static final String BASE_PACKAGE = SpringTypes.BASE_PACKAGE + ".data";

		static final String REPOSITORY = BASE_PACKAGE + ".repository.Repository";
		static final String AT_REPOSITORY_DEFINITION = BASE_PACKAGE + ".repository.RepositoryDefinition";

		static boolean isPresent() {
			return ClassUtils.isPresent(REPOSITORY, SpringDataTypes.class.getClassLoader());
		}

		static DescribedPredicate<JavaClass> isSpringDataRepository() {
			return assignableTo(SpringDataTypes.REPOSITORY) //
					.or(isAnnotatedWith(SpringDataTypes.AT_REPOSITORY_DEFINITION));
		}
	}

	DescribedPredicate<CanBeAnnotated> isAnnotatedWith(Class<?> type) {
		return isAnnotatedWith(type.getName());
	}

	DescribedPredicate<CanBeAnnotated> isAnnotatedWith(String type) {
		return Predicates.annotatedWith(type) //
				.or(Predicates.metaAnnotatedWith(type));
	}
}
