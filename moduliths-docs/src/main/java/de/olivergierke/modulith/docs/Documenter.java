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

import de.olivergierke.moduliths.model.Module;
import de.olivergierke.moduliths.model.Module.DependencyType;
import de.olivergierke.moduliths.model.Modules;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.structurizr.Workspace;
import com.structurizr.io.plantuml.PlantUMLWriter;
import com.structurizr.model.Component;
import com.structurizr.model.Container;
import com.structurizr.model.Model;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.view.ComponentView;
import com.structurizr.view.ViewSet;

/**
 * @author Oliver Gierke
 */
public class Documenter {

	private static final String DEFAULT_LOCATION = "target/generated-docs/moduliths";

	private final Workspace workspace;
	private final Container container;
	private final Map<Module, Component> components;

	public Documenter(Class<?> modulithType) {
		this(Modules.of(modulithType));
	}

	public Documenter(Modules modules) {

		this.workspace = new Workspace("Modulith", "");

		Model model = workspace.getModel();

		SoftwareSystem system = model.addSoftwareSystem("Modulith", "");

		this.container = system.addContainer("Application", "", "");
		this.components = StreamSupport.stream(modules.spliterator(), false) //
				.collect(Collectors.toMap(Function.identity(), it -> container.addComponent(it.getDisplayName(), "")));

		this.components.forEach((module, component) -> {
			module.getBootstrapDependencies(modules) //
					.forEach(it -> component.uses(components.get(it), "uses"));
			module.getDependencies(modules, DependencyType.EVENT_LISTENER) //
					.forEach(it -> component.uses(components.get(it), "listens to"));
			module.getDependencies(modules, DependencyType.DEFAULT) //
					.forEach(it -> component.uses(components.get(it), "depends on"));
		});
	}

	public void writePlantUml() throws IOException {

		Files.createDirectories(Paths.get(DEFAULT_LOCATION));

		Path filePath = Paths.get(DEFAULT_LOCATION, "components.uml");

		Files.deleteIfExists(filePath);

		Path file = Files.createFile(filePath);

		try (Writer writer = new FileWriter(file.toFile())) {
			createPlantUml(writer);
		}
	}

	public String toPlantUml() throws IOException {
		return createPlantUml(new StringWriter()).toString();
	}

	private <T extends Writer> T createPlantUml(T writer) throws IOException {

		ViewSet views = workspace.getViews();
		PlantUMLWriter plantUml = new PlantUMLWriter();
		ComponentView componentView = views.createComponentView(container, "modules", "");

		componentView.addAllComponents();

		plantUml.write(componentView, writer);

		return writer;
	}

}
