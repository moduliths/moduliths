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

import static org.moduliths.model.Types.JavaXTypes.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.moduliths.model.Types.SpringDataTypes;
import org.moduliths.model.Types.SpringTypes;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.util.ClassUtils;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.thirdparty.com.google.common.base.Supplier;
import com.tngtech.archunit.thirdparty.com.google.common.base.Suppliers;

/**
 * A type that is architecturally relevant, i.e. it fulfils a significant role within the architecture.
 *
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ArchitecturallyEvidentType {

	private final @Getter JavaClass type;

	/**
	 * Creates a new {@link ArchitecturallyEvidentType} for the given {@link JavaType} and {@link Classes} of Spring
	 * components.
	 *
	 * @param type must not be {@literal null}.
	 * @param beanTypes must not be {@literal null}.
	 * @return
	 */
	public static ArchitecturallyEvidentType of(JavaClass type, Classes beanTypes) {

		return ClassUtils.isPresent(SpringDataTypes.REPOSITORY, ArchitecturallyEvidentType.class.getClassLoader())
				? new SpringDataAwareArchitecturallyEvidentType(type, beanTypes) //
				: new SpringAwareArchitecturallyEvidentType(type);
	}

	/**
	 * Returns the abbreviated (i.e. every package fragment reduced to its first character) full name.
	 *
	 * @return will never be {@literal null}.
	 */
	public String getAbbreviatedFullName() {
		return FormatableJavaClass.of(type).getAbbreviatedFullName();
	}

	/**
	 * Returns whether the type is an entity in the DDD sense.
	 *
	 * @return
	 */
	public boolean isEntity() {
		return isJpaEntity().apply(getType());
	}

	/**
	 * Returns whether the type is considered an aggregate root in the DDD sense.
	 *
	 * @return
	 */
	public abstract boolean isAggregateRoot();

	/**
	 * Returns whether the type is considered a repository in the DDD sense.
	 *
	 * @return
	 */
	public abstract boolean isRepository();

	static class SpringAwareArchitecturallyEvidentType extends ArchitecturallyEvidentType {

		/**
		 * Creates a new {@link SpringDataAwareArchitecturallyEvidentType} for the given {@link JavaClass}.
		 */
		SpringAwareArchitecturallyEvidentType(JavaClass type) {
			super(type);
		}

		@Override
		public boolean isAggregateRoot() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isRepository()
		 */
		@Override
		public boolean isRepository() {
			return isSpringRepository().apply(getType());
		}

		/**
		 * Returns a {@link DescribedPredicate}
		 *
		 * @return
		 */
		protected DescribedPredicate<? super JavaClass> isSpringRepository() {
			return CanBeAnnotated.Predicates.annotatedWith(SpringTypes.AT_REPOSITORY);
		}
	}

	static class SpringDataAwareArchitecturallyEvidentType extends SpringAwareArchitecturallyEvidentType {

		private final Supplier<Boolean> isAggregate;

		SpringDataAwareArchitecturallyEvidentType(JavaClass type, Classes beanTypes) {

			super(type);

			this.isAggregate = Suppliers.memoize(() -> {

				return isEntity() && beanTypes.that(SpringDataTypes.isSpringDataRepository()).stream() //
						.map(JavaClass::reflect) //
						.map(AbstractRepositoryMetadata::getMetadata) //
						.map(RepositoryMetadata::getDomainType) //
						.anyMatch(type::isAssignableTo);
			});
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEntity()
		 */
		@Override
		public boolean isEntity() {

			return super.isEntity() //
					|| getType().isAnnotatedWith("org.springframework.data.mongodb.core.mapping.Document");
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.DefaultArchitectuallyEvidentType#isAggregateRoot(org.moduliths.model.Classes)
		 */
		@Override
		public boolean isAggregateRoot() {
			return isAggregate.get();
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.DefaultArchitectuallyEvidentType#isSpringRepository()
		 */
		@Override
		protected DescribedPredicate<? super JavaClass> isSpringRepository() {
			return SpringDataTypes.isSpringDataRepository().or(super.isSpringRepository());
		}
	}
}
