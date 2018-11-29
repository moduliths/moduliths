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
import de.olivergierke.moduliths.model.Module.DependencyDepth;
import de.olivergierke.moduliths.model.Module.DependencyType;
import de.olivergierke.moduliths.model.Modules;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.Assert;

import com.structurizr.Workspace;
import com.structurizr.io.plantuml.PlantUMLWriter;
import com.structurizr.model.Component;
import com.structurizr.model.Container;
import com.structurizr.model.Element;
import com.structurizr.model.Model;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.view.ComponentView;
import com.structurizr.view.RelationshipView;
import com.structurizr.view.View;

/**
 * API to create documentation for {@link Modules}.
 * 
 * @author Oliver Gierke
 */
public class Documenter {

	private static final String DEFAULT_LOCATION = "target/generated-docs/moduliths";
	private static final Map<DependencyType, String> DEPENDENCY_DESCRIPTIONS = new LinkedHashMap<>();

	private static final String INVALID_FILE_NAME_PATTERN = "Configured file name pattern does not include a '%s' placeholder for the module name!";

	static {
		DEPENDENCY_DESCRIPTIONS.put(DependencyType.EVENT_LISTENER, "listens to");
		DEPENDENCY_DESCRIPTIONS.put(DependencyType.DEFAULT, "depends on");
	}

	private final @Getter Modules modules;
	private final Workspace workspace;
	private final Container container;
	private final Map<Module, Component> components;

	/**
	 * Creates a new {@link Documenter} for the {@link Modules} created for the given modulith type.
	 * 
	 * @param modulithType must not be {@literal null}.
	 */
	public Documenter(Class<?> modulithType) {
		this(Modules.of(modulithType));
	}

	/**
	 * Creates a new {@link Documenter} for the given {@link Modules} instance.
	 * 
	 * @param modules must not be {@literal null}.
	 */
	public Documenter(Modules modules) {

		Assert.notNull(modules, "Modules must not be null!");

		this.modules = modules;
		this.workspace = new Workspace("Modulith", "");

		Model model = workspace.getModel();
		String systemName = modules.getSystemName().orElse("Modulith");

		SoftwareSystem system = model.addSoftwareSystem(systemName, "");

		this.container = system.addContainer("Application", "", "");
		this.components = modules.stream() //
				.collect(
						Collectors.toMap(Function.identity(), it -> container.addComponent(it.getDisplayName(), "", "Module")));

		this.components.forEach(this::addDependencies);
	}

	/**
	 * Writes the PlantUML component diagram for all {@link Modules}.
	 * 
	 * @param options must not be {@literal null}.
	 * @throws IOException
	 */
	public void writeModulesAsPlantUml(Options options) throws IOException {

		Assert.notNull(options, "Options must not be null!");

		Path file = recreateFile(options.getTargetFileName().orElse("components.uml"));

		try (Writer writer = new FileWriter(file.toFile())) {
			createPlantUml(writer, options);
		}
	}

	/**
	 * Writes the PlantUML component diagram for the given {@link Module}.
	 * 
	 * @param module must not be {@literal null}.
	 */
	public void writeModuleAsPlantUml(Module module) {

		Assert.notNull(module, "Module must not be null!");

		writeModuleAsPlantUml(module, Options.defaults());
	}

	/**
	 * Writes the PlantUML component diagram for the given {@link Module} with the given rendering {@link Options}.
	 * 
	 * @param module must not be {@literal null}.
	 * @param options must not be {@literal null}.
	 */
	public void writeModuleAsPlantUml(Module module, Options options) {

		Assert.notNull(module, "Module must not be null!");
		Assert.notNull(options, "Options must not be null!");

		ComponentView view = workspace.getViews().createComponentView(container, module.getDisplayName(), "");

		addComponentsToView(module, view, options);

		String fileNamePattern = options.getTargetFileName().orElse("module-%s.uml");

		Assert.isTrue(fileNamePattern.contains("%s"), () -> String.format(INVALID_FILE_NAME_PATTERN, fileNamePattern));

		writeViewAsPlantUml(view, String.format(fileNamePattern, module.getName()));
	}

	public String toPlantUml() throws IOException {
		return createPlantUml(new StringWriter(), Options.defaults()).toString();
	}

	private void addDependencies(Module module, Component component) {

		DEPENDENCY_DESCRIPTIONS.entrySet().stream().forEach(entry -> {

			module.getDependencies(modules, entry.getKey()).stream() //
					.map(components::get) //
					// .filter(it -> !component.hasEfferentRelationshipWith(it)) //
					.forEach(it -> {

						Relationship relationship = component.uses(it, entry.getValue());
						relationship.addTags(entry.getKey().toString());
					});
		});

		module.getBootstrapDependencies(modules) //
				.forEach(it -> {
					Relationship relationship = component.uses(components.get(it), "uses");
					relationship.addTags(DependencyType.USES_COMPONENT.toString());
				});
	}

	private void addComponentsToView(Module module, ComponentView view, Options options) {

		Stream<Module> bootstrapDependencies = module.getBootstrapDependencies(modules, options.getDependencyDepth());
		Stream<Module> otherDependencies = options.getDependencyTypes()
				.flatMap(it -> module.getDependencies(modules, it).stream());

		Stream<Module> dependencies = Stream.concat(bootstrapDependencies, otherDependencies);

		addComponentsToView(dependencies, view, options, it -> it.add(components.get(module)));
	}

	private void addComponentsToView(Stream<Module> modules, ComponentView view, Options options,
			Consumer<ComponentView> afterCleanup) {

		modules.filter(options.getExclusions().negate()) //
				.map(components::get) //
				.filter(options.getComponentFilter()).forEach(view::add);

		// Remove filtered dependency types
		DependencyType.allBut(options.getDependencyTypes()) //
				.map(Object::toString) //
				.forEach(it -> view.removeRelationshipsWithTag(it));

		afterCleanup.accept(view);

		// … as well as all elements left without a relationship
		view.removeElementsWithNoRelationships();

		afterCleanup.accept(view);

		// Remove default relationships if more qualified ones exist
		view.getRelationships().stream() //
				.map(RelationshipView::getRelationship) //
				.collect(Collectors.groupingBy(Connection::of)) //
				.values().stream() //
				.forEach(it -> potentiallyRemoveDefaultRelationship(view, it));
	}

	private void potentiallyRemoveDefaultRelationship(View view, Collection<Relationship> relationships) {

		if (relationships.size() <= 1) {
			return;
		}

		relationships.stream().filter(it -> it.getTagsAsSet().contains(DependencyType.DEFAULT.toString())) //
				.findFirst().ifPresent(view::remove);
	}

	@Value
	private static class Connection {

		Element source, target;

		public static Connection of(Relationship relationship) {
			return new Connection(relationship.getSource(), relationship.getDestination());
		}
	}

	private void writeViewAsPlantUml(View view, String filename) {

		try {

			Path file = recreateFile(filename);

			try (Writer writer = new FileWriter(file.toFile())) {
				new PlantUMLWriter().write(view, writer);
			}

		} catch (IOException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private <T extends Writer> T createPlantUml(T writer, Options options) throws IOException {

		ComponentView componentView = workspace.getViews() //
				.createComponentView(container, "modules-" + options.toString(), "");

		addComponentsToView(modules.stream(), componentView, options, it -> {});

		new PlantUMLWriter().write(componentView, writer);

		return writer;
	}

	private static Path recreateFile(String name) {

		try {

			Files.createDirectories(Paths.get(DEFAULT_LOCATION));

			Path filePath = Paths.get(DEFAULT_LOCATION, name);

			Files.deleteIfExists(filePath);

			return Files.createFile(filePath);

		} catch (IOException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	/**
	 * Options to tweak the rendering of diagrams.
	 *
	 * @author Oliver Gierke
	 */
	@Getter(AccessLevel.PRIVATE)
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Options {

		private static Set<DependencyType> ALL_TYPES = Arrays.stream(DependencyType.values()).collect(Collectors.toSet());

		private final Set<DependencyType> dependencyTypes;

		/**
		 * The {@link DependencyDepth} to define which other modules to be included in the diagram to be created.
		 */
		private final @Wither DependencyDepth dependencyDepth;

		/**
		 * A {@link Predicate} to define the which modules to exclude from the diagram to be created.
		 */
		private final @Wither Predicate<Module> exclusions;

		/**
		 * A Predicate to define which Structurizr {@link Component}s to be included in the diagram to be created.
		 */
		private final @Wither Predicate<Component> componentFilter;

		/**
		 * The target file name to be used for the diagram to be created. For individual module diagrams this needs to
		 * include a {@code %s} placeholder for the module names.
		 */
		private final @Wither String targetFileName;

		/**
		 * Creates a new default {@link Options} instance configured to use all dependency types, list immediate
		 * dependencies for individual module instances, not applying any kind of {@link Module} or {@link Component}
		 * filters and default file names.
		 * 
		 * @return will never be {@literal null}.
		 */
		public static Options defaults() {
			return new Options(ALL_TYPES, DependencyDepth.IMMEDIATE, it -> false, it -> true, null);
		}

		/**
		 * Select the dependency types that are supposed to be included in the diagram to be created.
		 * 
		 * @param types must not be {@literal null}.
		 * @return
		 */
		public Options withDependencyTypes(DependencyType... types) {

			Assert.notNull(types, "Dependency types must not be null!");

			Set<DependencyType> dependencyTypes = Arrays.stream(types).collect(Collectors.toSet());
			return new Options(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetFileName);
		}

		private Optional<String> getTargetFileName() {
			return Optional.ofNullable(targetFileName);
		}

		private Stream<DependencyType> getDependencyTypes() {
			return dependencyTypes.stream();
		}
	}
}
