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
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jddd.core.annotation.AggregateRoot;
import org.jddd.core.annotation.Entity;
import org.jddd.core.annotation.Repository;
import org.jddd.core.annotation.Service;
import org.moduliths.model.Types.JDDDTypes;
import org.moduliths.model.Types.JMoleculesTypes;
import org.moduliths.model.Types.SpringDataTypes;
import org.moduliths.model.Types.SpringTypes;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.thirdparty.com.google.common.base.Supplier;
import com.tngtech.archunit.thirdparty.com.google.common.base.Suppliers;

/**
 * A type that is architecturally relevant, i.e. it fulfills a significant role within the architecture.
 *
 * @author Oliver Drotbohm
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ArchitecturallyEvidentType {

	private static boolean deprecationWarningLogged = false;
	private static Map<Key, ArchitecturallyEvidentType> CACHE = new HashMap<>();

	private final @Getter JavaClass type;

	/**
	 * Creates a new {@link AbstractArchitecturallyEvidentType} for the given {@link JavaType} and {@link Classes} of
	 * Spring components.
	 *
	 * @param type must not be {@literal null}.
	 * @param beanTypes must not be {@literal null}.
	 * @return
	 */
	public static ArchitecturallyEvidentType of(JavaClass type, Classes beanTypes) {

		return CACHE.computeIfAbsent(Key.of(type, beanTypes), it -> {

			List<ArchitecturallyEvidentType> delegates = new ArrayList<>();

			if (JDDDTypes.isPresent()) {

				if (!deprecationWarningLogged) {
					LOG.warn("jDDD support in Moduliths is deprecated. Please move to jMolecules (http://jmolecules.org).");
					deprecationWarningLogged = true;
				}

				delegates.add(new JDdddArchitecturallyEvidentType(type));
			}

			if (JMoleculesTypes.isPresent()) {
				delegates.add(new JMoleculesArchitecturallyEvidentType(type));
			}

			if (SpringDataTypes.isPresent()) {
				delegates.add(new SpringDataAwareArchitecturallyEvidentType(type, beanTypes));
			}

			delegates.add(new SpringAwareArchitecturallyEvidentType(type));

			return DelegatingType.of(type, delegates);
		});
	}

	/**
	 * Returns the abbreviated (i.e. every package fragment reduced to its first character) full name.
	 *
	 * @return will never be {@literal null}.
	 */
	String getAbbreviatedFullName() {
		return FormatableJavaClass.of(getType()).getAbbreviatedFullName();
	}

	/**
	 * Returns whether the type is an entity in the DDD sense.
	 *
	 * @return
	 */
	boolean isEntity() {
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

	public boolean isService() {
		return false;
	}

	public boolean isController() {
		return false;
	}

	public boolean isEventListener() {
		return false;
	}

	/**
	 * Returns other types that are interesting in the context of the current {@link ArchitecturallyEvidentType}. For
	 * example, for an event listener this might be the event types the particular listener is interested in.
	 *
	 * @return
	 */
	public Stream<JavaClass> getReferenceTypes() {
		return Stream.empty();
	}

	static class SpringAwareArchitecturallyEvidentType extends ArchitecturallyEvidentType {

		private static final Predicate<JavaMethod> IS_EVENT_LISTENER = it -> it
				.tryGetAnnotationOfType(SpringTypes.AT_EVENT_LISTENER).isPresent()
				|| it.tryGetAnnotationOfType(SpringTypes.AT_TX_EVENT_LISTENER).isPresent();

		public SpringAwareArchitecturallyEvidentType(JavaClass type) {
			super(type);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isAggregateRoot()
		 */
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
			return Types.isAnnotatedWith(SpringTypes.AT_REPOSITORY).apply(getType());
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isService()
		 */
		@Override
		public boolean isService() {
			return Types.isAnnotatedWith(SpringTypes.AT_SERVICE).apply(getType());
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isController()
		 */
		@Override
		public boolean isController() {
			return Types.isAnnotatedWith(SpringTypes.AT_CONTROLLER).apply(getType());
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEventListener()
		 */
		@Override
		public boolean isEventListener() {
			return getType().getMethods().stream().anyMatch(IS_EVENT_LISTENER);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#getOtherTypeReferences()
		 */
		@Override
		public Stream<JavaClass> getReferenceTypes() {

			if (isEventListener()) {

				return distinctByName(getType().getMethods().stream() //
						.filter(IS_EVENT_LISTENER) //
						.flatMap(it -> it.getRawParameterTypes().stream()))
								.sorted(Comparator.comparing(JavaClass::getSimpleName));
			}

			return super.getReferenceTypes();
		}
	}

	private static Stream<JavaClass> distinctByName(Stream<JavaClass> types) {

		Set<String> names = new HashSet<>();

		return types.flatMap(it -> {

			if (names.contains(it.getFullName())) {
				return Stream.empty();
			} else {

				names.add(it.getFullName());

				return Stream.of(it);
			}
		});
	}

	static class SpringDataAwareArchitecturallyEvidentType extends ArchitecturallyEvidentType {

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
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isRepository()
		 */
		@Override
		public boolean isRepository() {
			return SpringDataTypes.isSpringDataRepository().apply(getType());
		}
	}

	static class JDdddArchitecturallyEvidentType extends ArchitecturallyEvidentType {

		private JDdddArchitecturallyEvidentType(JavaClass type) {
			super(type);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEntity()
		 */
		@Override
		public boolean isEntity() {

			return Types.isAnnotatedWith(Entity.class).apply(getType()) || //
					getType().isAssignableTo(org.jddd.core.types.Entity.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isAggregateRoot()
		 */
		@Override
		public boolean isAggregateRoot() {

			return Types.isAnnotatedWith(AggregateRoot.class).apply(getType()) || //
					getType().isAssignableTo(org.jddd.core.types.AggregateRoot.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isRepository()
		 */
		@Override
		public boolean isRepository() {
			return Types.isAnnotatedWith(Repository.class).apply(getType());
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isService()
		 */
		@Override
		public boolean isService() {
			return Types.isAnnotatedWith(Service.class).apply(getType());
		}
	}

	static class JMoleculesArchitecturallyEvidentType extends ArchitecturallyEvidentType {

		JMoleculesArchitecturallyEvidentType(JavaClass type) {
			super(type);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEntity()
		 */
		@Override
		public boolean isEntity() {

			JavaClass type = getType();

			return Types.isAnnotatedWith(org.jmolecules.ddd.annotation.Entity.class).apply(type) || //
					type.isAssignableTo(org.jmolecules.ddd.types.Entity.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isAggregateRoot()
		 */
		@Override
		public boolean isAggregateRoot() {

			JavaClass type = getType();

			return Types.isAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class).apply(type) || //
					type.isAssignableTo(org.jmolecules.ddd.types.AggregateRoot.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isRepository()
		 */
		@Override
		public boolean isRepository() {

			JavaClass type = getType();

			return Types.isAnnotatedWith(org.jmolecules.ddd.annotation.Repository.class).apply(type);
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isService()
		 */
		@Override
		public boolean isService() {

			JavaClass type = getType();

			return Types.isAnnotatedWith(org.jmolecules.ddd.annotation.Service.class).apply(type);
		}
	}

	static class DelegatingType extends ArchitecturallyEvidentType {

		private final Supplier<Boolean> isAggregateRoot, isRepository, isEntity, isService, isController, isEventListener;
		private final Supplier<Collection<JavaClass>> referenceTypes;

		DelegatingType(JavaClass type, Supplier<Boolean> isAggregateRoot,
				Supplier<Boolean> isRepository, Supplier<Boolean> isEntity, Supplier<Boolean> isService,
				Supplier<Boolean> isController, Supplier<Boolean> isEventListener,
				Supplier<Collection<JavaClass>> referenceTypes) {

			super(type);

			this.isAggregateRoot = isAggregateRoot;
			this.isRepository = isRepository;
			this.isEntity = isEntity;
			this.isService = isService;
			this.isController = isController;
			this.isEventListener = isEventListener;
			this.referenceTypes = referenceTypes;
		}

		public static DelegatingType of(JavaClass type, List<ArchitecturallyEvidentType> types) {

			Supplier<Boolean> isAggregateRoot = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isAggregateRoot));

			Supplier<Boolean> isRepository = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isRepository));

			Supplier<Boolean> isEntity = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isEntity));

			Supplier<Boolean> isService = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isService));

			Supplier<Boolean> isController = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isController));

			Supplier<Boolean> isEventListener = Suppliers
					.memoize(() -> types.stream().anyMatch(ArchitecturallyEvidentType::isEventListener));

			Supplier<Collection<JavaClass>> referenceTypes = Suppliers.memoize(() -> types.stream() //
					.flatMap(ArchitecturallyEvidentType::getReferenceTypes) //
					.collect(Collectors.toList()));

			return new DelegatingType(type, isAggregateRoot, isRepository, isEntity, isService, isController,
					isEventListener, referenceTypes);
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

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isService()
		 */
		@Override
		public boolean isService() {
			return isService.get();
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isController()
		 */
		@Override
		public boolean isController() {
			return isController.get();
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#isEventListener()
		 */
		@Override
		public boolean isEventListener() {
			return isEventListener.get();
		}

		/*
		 * (non-Javadoc)
		 * @see org.moduliths.model.ArchitecturallyEvidentType#getOtherTypeReferences()
		 */
		@Override
		public Stream<JavaClass> getReferenceTypes() {
			return distinctByName(referenceTypes.get().stream());
		}
	}

	@Value(staticConstructor = "of")
	private static class Key {

		JavaClass type;
		Classes beanTypes;
	}
}
