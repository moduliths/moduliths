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
package de.olivergierke.modulith.docs;

import de.olivergierke.modulith.docs.Documenter.Options;
import de.olivergierke.moduliths.model.Module;
import de.olivergierke.moduliths.model.Module.DependencyType;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;

import com.acme.myproject.Application;

/**
 * Unit tests for {@link Documenter}.
 *
 * @author Oliver Gierke
 */
public class DocumenterTest {

	Documenter documenter = new Documenter(Application.class);

	@Test
	public void writesComponentStructureAsPlantUml() throws IOException {
		documenter.toPlantUml();
	}

	@Test
	public void writesSingleModuleDocumentation() throws IOException {

		Module module = documenter.getModules().getModuleByName("moduleB") //
				.orElseThrow(() -> new IllegalArgumentException());

		documenter.writeModuleAsPlantUml(module, Options.defaults() //
				.withColorSelector(it -> Optional.of("#ff0000")) //
				.withDefaultDisplayName(it -> it.getDisplayName().toUpperCase()));

		Options options = Options.defaults() //
				.withComponentFilter(component -> component.getRelationships().stream()
						.anyMatch(it -> it.getTagsAsSet().contains(DependencyType.EVENT_LISTENER.toString())));

		documenter.writeModulesAsPlantUml(options);
	}
}
