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

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.moduliths.model.ArchitecturallyEvidentType.SpringAwareArchitecturallyEvidentType;
import org.moduliths.model.ArchitecturallyEvidentType.SpringDataAwareArchitecturallyEvidentType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Unit tests for {@link ArchitecturallyEvidentType}.
 *
 * @author Oliver Drotbohm
 */
class ArchitecturallyEvidentTypeUnitTest {

	Classes classes = TestUtils.getClasses();
	JavaClass self = classes.getRequiredClass(ArchitecturallyEvidentTypeUnitTest.class);

	@Test // #101
	void abbreviatesFullyQualifiedTypeName() {

		ArchitecturallyEvidentType type = ArchitecturallyEvidentType.of(self, classes);

		assertThat(type.getAbbreviatedFullName()).isEqualTo("o.m.m.ArchitecturallyEvidentTypeUnitTest");
	}

	@Test // #101
	void doesNotConsiderArbitraryTypeAStereotype() {

		ArchitecturallyEvidentType type = ArchitecturallyEvidentType.of(self, classes);

		assertThat(type.isEntity()).isFalse();
		assertThat(type.isAggregateRoot()).isFalse();
		assertThat(type.isRepository()).isFalse();
	}

	@Test // #101
	void detectsSpringAnnotatedRepositories() {

		ArchitecturallyEvidentType type = new SpringAwareArchitecturallyEvidentType(
				classes.getRequiredClass(SpringRepository.class));

		assertThat(type.isRepository()).isTrue();
	}

	@Test // #101
	void doesNotConsiderEntityAggregateRoot() {

		ArchitecturallyEvidentType type = new SpringAwareArchitecturallyEvidentType(
				classes.getRequiredClass(SampleEntity.class));

		assertThat(type.isEntity()).isTrue();
		assertThat(type.isAggregateRoot()).isFalse();
	}

	@Test // #101
	void considersEntityAnAggregateRootIfTheresARepositoryForIt() {

		Map<Class<?>, Boolean> parameters = new HashMap<Class<?>, Boolean>();
		parameters.put(SampleEntity.class, true);
		parameters.put(OtherEntity.class, false);
		parameters.put(NoEntity.class, false);

		parameters.entrySet().stream().forEach(it -> {

			JavaClass entity = classes.getRequiredClass(it.getKey());

			assertThat(new SpringDataAwareArchitecturallyEvidentType(entity, classes).isAggregateRoot())
					.isEqualTo(it.getValue());
		});
	}

	// Spring

	@Repository
	interface SpringRepository {}

	@Entity
	class SampleEntity {}

	// Spring Data

	interface SampleRepository extends CrudRepository<SampleEntity, UUID> {}

	@Entity
	class OtherEntity {}

	class NoEntity {}
}
