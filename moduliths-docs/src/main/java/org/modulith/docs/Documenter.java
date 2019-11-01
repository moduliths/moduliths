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
package org.modulith.docs;

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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.moduliths.model.Module;
import org.moduliths.model.Modules;
import org.moduliths.model.Module.DependencyDepth;
import org.moduliths.model.Module.DependencyType;
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

	private Map<Module, Component> components;

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
	}

	private Map<Module, Component> getComponents(Options options) {

		if (components == null) {

			this.components = modules.stream() //
					.collect(Collectors.toMap(Function.identity(),
							it -> container.addComponent(options.getDefaultDisplayName().apply(it), "", "Module")));

			this.components.forEach((key, value) -> addDependencies(key, value, options));
		}

		return components;
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

		ComponentView view = workspace.getViews().createComponentView(container, module.getName(), "");
		view.setTitle(options.getDefaultDisplayName().apply(module));

		addComponentsToView(module, view, options);

		String fileNamePattern = options.getTargetFileName().orElse("module-%s.uml");

		Assert.isTrue(fileNamePattern.contains("%s"), () -> String.format(INVALID_FILE_NAME_PATTERN, fileNamePattern));

		writeViewAsPlantUml(view, String.format(fileNamePattern, module.getName()), options);
	}

	public String toPlantUml() throws IOException {
		return createPlantUml(new StringWriter(), Options.defaults()).toString();
	}

	private void addDependencies(Module module, Component component, Options options) {

		DEPENDENCY_DESCRIPTIONS.entrySet().stream().forEach(entry -> {

			module.getDependencies(modules, entry.getKey()).stream() //
					.map(it -> getComponents(options).get(it)) //
					// .filter(it -> !component.hasEfferentRelationshipWith(it)) //
					.forEach(it -> {

						Relationship relationship = component.uses(it, entry.getValue());
						relationship.addTags(entry.getKey().toString());
					});
		});

		module.getBootstrapDependencies(modules) //
				.forEach(it -> {
					Relationship relationship = component.uses(getComponents(options).get(it), "uses");
					relationship.addTags(DependencyType.USES_COMPONENT.toString());
				});
	}

	private void addComponentsToView(Module module, ComponentView view, Options options) {

		Supplier<Stream<Module>> bootstrapDependencies = () -> module.getBootstrapDependencies(modules,
				options.getDependencyDepth());
		Supplier<Stream<Module>> otherDependencies = () -> options.getDependencyTypes()
				.flatMap(it -> module.getDependencies(modules, it).stream());

		Supplier<Stream<Module>> dependencies = () -> Stream.concat(bootstrapDependencies.get(), otherDependencies.get());

		addComponentsToView(dependencies, view, options, it -> it.add(getComponents(options).get(module)));
	}

	private void addComponentsToView(Supplier<Stream<Module>> modules, ComponentView view, Options options,
			Consumer<ComponentView> afterCleanup) {

		modules.get().filter(options.getExclusions().negate()) //
				.map(it -> getComponents(options).get(it)) //
				.filter(options.getComponentFilter()).forEach(view::add);

		// Remove filtered dependency types
		DependencyType.allBut(options.getDependencyTypes()) //
				.map(Object::toString) //
				.forEach(it -> view.removeRelationshipsWithTag(it));

		afterCleanup.accept(view);

		// Filter outgoing relationships of target-only modules
		modules.get().filter(options.getTargetOnly()) //
				.forEach(module -> {

					Component component = getComponents(options).get(module);

					view.getRelationships().stream() //
							.map(RelationshipView::getRelationship) //
							.filter(it -> it.getSource().equals(component)) //
							.forEach(it -> view.remove(it));
				});

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

	private void writeViewAsPlantUml(View view, String filename, Options options) {

		try {

			Path file = recreateFile(filename);

			try (Writer writer = new FileWriter(file.toFile())) {
				getPlantUMLWriter(options).write(view, writer);
			}

		} catch (IOException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	private PlantUMLWriter getPlantUMLWriter(Options options) {
		return new CustomPlantUmlWriter(options.getColorSelector(), getComponents(options));
	}

	private <T extends Writer> T createPlantUml(T writer, Options options) throws IOException {

		ComponentView componentView = workspace.getViews() //
				.createComponentView(container, "modules-" + options.toString(), "");
		componentView.setTitle(modules.getSystemName().orElse("Modules"));

		addComponentsToView(() -> modules.stream(), componentView, options, it -> {});

		getPlantUMLWriter(options).write(componentView, writer);

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
		 * A {@link Predicate} to define which Structurizr {@link Component}s to be included in the diagram to be created.
		 */
		private final @Wither Predicate<Component> componentFilter;

		/**
		 * A {@link Predicate} to define which of the modules shall only be considered targets, i.e. all efferent
		 * relationships are going to be hidden from the rendered view. Modules that have no incoming relationships will
		 * entirely be removed from the view.
		 */
		private final @Wither Predicate<Module> targetOnly;

		/**
		 * The target file name to be used for the diagram to be created. For individual module diagrams this needs to
		 * include a {@code %s} placeholder for the module names.
		 */
		private final @Wither String targetFileName;

		/**
		 * A callback to return a hex-encoded color per {@link Module}.
		 */
		private final @Wither Function<Module, Optional<String>> colorSelector;

		/**
		 * A callback to return a default display names for a given {@link Module}. Default implementation just forwards to
		 * {@link Module#getDisplayName()}.
		 */
		private final @Wither Function<Module, String> defaultDisplayName;

		/**
		 * Creates a new default {@link Options} instance configured to use all dependency types, list immediate
		 * dependencies for individual module instances, not applying any kind of {@link Module} or {@link Component}
		 * filters and default file names.
		 *
		 * @return will never be {@literal null}.
		 */
		public static Options defaults() {
			return new Options(ALL_TYPES, DependencyDepth.IMMEDIATE, it -> false, it -> true, it -> false, null,
					__ -> Optional.empty(), it -> it.getDisplayName());
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

			return new Options(dependencyTypes, dependencyDepth, exclusions, componentFilter, targetOnly, targetFileName,
					colorSelector, defaultDisplayName);
		}

		private Optional<String> getTargetFileName() {
			return Optional.ofNullable(targetFileName);
		}

		private Stream<DependencyType> getDependencyTypes() {
			return dependencyTypes.stream();
		}
	}

	/**
	 * Custom {@link PlantUMLWriter} to apply the {@link Options#getColorSelector()}s while writing the component
	 * instances into the PlantUML component diagram.
	 *
	 * @author Oliver Drotbohm
	 */
	@RequiredArgsConstructor
	private static class CustomPlantUmlWriter extends PlantUMLWriter {

		private final Function<Module, Optional<String>> colorSelector;
		private final Map<Module, Component> components;

		/*
		 * (non-Javadoc)
		 * @see com.structurizr.io.plantuml.PlantUMLWriter#backgroundOf(com.structurizr.view.View, com.structurizr.model.Element)
		 */
		@Override
		protected String backgroundOf(View view, Element element) {

			if (!Component.class.isInstance(element)) {
				return super.backgroundOf(view, element);
			}

			Component component = (Component) element;

			return components.entrySet().stream() //
					.filter(it -> it.getValue().equals(component)) //
					.map(Entry::getKey) //
					.findFirst() //
					.flatMap(colorSelector) //
					.orElseGet(() -> super.backgroundOf(view, element));
		}
	}
}
