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

import java.util.ArrayList;
import java.util.List;

import org.jddd.core.annotation.AggregateRoot;
import org.jddd.core.annotation.Entity;
import org.jddd.core.annotation.Repository;
import org.moduliths.model.Types.JDDDTypes;
import org.moduliths.model.Types.SpringDataTypes;
import org.moduliths.model.Types.SpringTypes;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;

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

public interface ArchitecturallyEvidentType {

	/**
	 * Creates a new {@link AbstractArchitecturallyEvidentType} for the given {@link JavaType} and {@link Classes} of
	 * Spring components.
	 *
	 * @param type must not be {@literal null}.
	 * @param beanTypes must not be {@literal null}.
	 * @return
	 */
	public static ArchitecturallyEvidentType of(JavaClass type, Classes beanTypes) {

		List<ArchitecturallyEvidentType> delegates = new ArrayList<>();

		if (JDDDTypes.isPresent()) {
			delegates.add(new JDdddArchitecturallyEvidentType(type));
		}

		if (SpringDataTypes.isPresent()) {
			delegates.add(new SpringDataAwareArchitecturallyEvidentType(type, beanTypes));
		}

		delegates.add(new SpringAwareArchitecturallyEvidentType(type));

		return DelegatingType.of(type, delegates);
	}

	public abstract JavaClass getType();

	/**
	 * Returns the abbreviated (i.e. every package fragment reduced to its first character) full name.
	 *
	 * @return will never be {@literal null}.
	 */
	default String getAbbreviatedFullName() {
		return FormatableJavaClass.of(getType()).getAbbreviatedFullName();
	}

	/**
	 * Returns whether the type is an entity in the DDD sense.
	 *
	 * @return
	 */
	default boolean isEntity() {
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

	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	static class SpringAwareArchitecturallyEvidentType implements ArchitecturallyEvidentType {

		private final @Getter JavaClass type;

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

		private final Classes beanTypes;

		SpringDataAwareArchitecturallyEvidentType(JavaClass type, Classes beanTypes) {

			super(type);

			this.beanTypes = beanTypes;
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
			return isEntity() && beanTypes.that(SpringDataTypes.isSpringDataRepository()).stream() //
					.map(JavaClass::reflect) //
					.map(AbstractRepositoryMetadata::getMetadata) //
					.map(RepositoryMetadata::getDomainType) //
					.anyMatch(it -> getType().isAssignableTo(it));
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

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	static class JDdddArchitecturallyEvidentType implements ArchitecturallyEvidentType {

		private final @Getter JavaClass type;

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEntity()
		 */
		@Override
		public boolean isEntity() {

			return Types.isAnnotatedWith(Entity.class).apply(type) || //
					type.isAssignableTo(org.jddd.core.types.Entity.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isAggregateRoot()
		 */
		@Override
		public boolean isAggregateRoot() {

			return Types.isAnnotatedWith(AggregateRoot.class).apply(type) || //
					type.isAssignableTo(org.jddd.core.types.AggregateRoot.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isRepository()
		 */
		@Override
		public boolean isRepository() {
			return Types.isAnnotatedWith(Repository.class).apply(type);
		}
	}

	@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
	static class DelegatingType implements ArchitecturallyEvidentType {

		private final @Getter JavaClass type;
		private final Supplier<Boolean> isAggregateRoot;
		private final Supplier<Boolean> isRepository;
		private final Supplier<Boolean> isEntity;

		public static DelegatingType of(JavaClass type, List<ArchitecturallyEvidentType> types) {

			Supplier<Boolean> isAggregateRoot = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isAggregateRoot));

			Supplier<Boolean> isRepository = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isRepository));

			Supplier<Boolean> isEntity = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isEntity));

			return new DelegatingType(type, isAggregateRoot, isRepository, isEntity);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isAggregateRoot()
		 */
		// @Override
		@Override
		public boolean isAggregateRoot() {
			return isAggregateRoot.get();
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isRepository()
		 */
		// @Override
		@Override
		public boolean isRepository() {
			return isRepository.get();
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEntity()
		 */
		@Override
		public boolean isEntity() {
			return isEntity.get();
		}
	}
}
