/*
 * Copyright 2018-2020 the original author or authors.
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
package org.moduliths.docs;

import static org.moduliths.docs.Asciidoctor.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.moduliths.model.Module;
import org.moduliths.model.Module.DependencyDepth;
import org.moduliths.model.Module.DependencyType;
import org.moduliths.model.Modules;
import org.moduliths.model.SpringBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * API to create documentation for {@link Modules}.
 *
 * @author Oliver Gierke
 */
public class Documenter {

	private static final String DEFAULT_LOCATION = "target/moduliths-docs";
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
	public Documenter writeModulesAsPlantUml(Options options) throws IOException {

		Assert.notNull(options, "Options must not be null!");

		Path file = recreateFile(options.getTargetFileName().orElse("components.uml"));

		try (Writer writer = new FileWriter(file.toFile())) {
			createPlantUml(writer, options);
		}

		return this;
	}

	/**
	 * Writes the PlantUML component diagram for the given {@link Module}.
	 *
	 * @param module must not be {@literal null}.
	 */
	public Documenter writeModuleAsPlantUml(Module module) {

		Assert.notNull(module, "Module must not be null!");

		return writeModuleAsPlantUml(module, Options.defaults());
	}

	/**
	 * Writes the PlantUML component diagram for the given {@link Module} with the given rendering {@link Options}.
	 *
	 * @param module must not be {@literal null}.
	 * @param options must not be {@literal null}.
	 */
	public Documenter writeModuleAsPlantUml(Module module, Options options) {

		Assert.notNull(module, "Module must not be null!");
		Assert.notNull(options, "Options must not be null!");

		ComponentView view = workspace.getViews().createComponentView(container, module.getName(), "");
		view.setTitle(options.getDefaultDisplayName().apply(module));

		addComponentsToView(module, view, options);

		String fileNamePattern = options.getTargetFileName().orElse("module-%s.uml");

		Assert.isTrue(fileNamePattern.contains("%s"), () -> String.format(INVALID_FILE_NAME_PATTERN, fileNamePattern));

		return writeViewAsPlantUml(view, String.format(fileNamePattern, module.getName()), options);
	}

	public Documenter writeModuleCanvases() {
		return writeModuleCanvases(CanvasOptions.defaults());
	}

	public Documenter writeModuleCanvases(CanvasOptions canvasOptions) {

		Options options = Options.defaults();

		modules.forEach(module -> {

			String filename = String.format(options.getTargetFileName().orElse("module-%s.adoc"), module.getName());
			Path file = recreateFile(filename);

			try (FileWriter writer = new FileWriter(file.toFile())) {

				writer.write(toModuleCanvas(module, canvasOptions));

			} catch (IOException o_O) {
				throw new RuntimeException(o_O);
			}
		});

		return this;
	}

	/**
	 * @param javadocBase
	 * @deprecated since 1.1, use {@link #writeModuleCanvases(CanvasOptions)} instead.
	 */
	@Deprecated
	public Documenter writeModuleCanvases(String javadocBase) {
		return writeModuleCanvases(CanvasOptions.defaults().withApiBase(javadocBase));
	}

	public String toModuleCanvas(Module module) {
		return toModuleCanvas(module, CanvasOptions.defaults());
	}

	public String toModuleCanvas(Module module, String apiBase) {
		return toModuleCanvas(module, CanvasOptions.defaults().withApiBase(apiBase));
	}

	public String toModuleCanvas(Module module, CanvasOptions options) {

		Asciidoctor asciidoctor = Asciidoctor.withJavadocBase(options.getApiBase(), module);
		Function<List<JavaClass>, String> mapper = asciidoctor::typesToBulletPoints;

		StringBuilder builder = new StringBuilder();
		builder.append(startTable("%autowidth.stretch, cols=\"h,a\""));
		builder.append(writeTableRow("Base package", asciidoctor.toInlineCode(module.getBasePackage().getName())));
		builder.append(writeTableRow("Spring components", asciidoctor.renderSpringBeans(options)));
		builder.append(addTableRow(module.getAggregateRoots(modules), "Aggregate roots", mapper));
		builder.append(addTableRow(module.getEventsPublished(), "Published events", mapper));
		builder.append(addTableRow(module.getEventsListenedTo(modules), "Events listened to", mapper));
		builder.append(startOrEndTable());

		return builder.toString();
	}

	private <T> String addTableRow(List<T> types, String header, Function<List<T>, String> mapper) {
		return types.isEmpty() ? "" : writeTableRow(header, mapper.apply(types));
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

	private Documenter writeViewAsPlantUml(View view, String filename, Options options) {

		try {

			Path file = recreateFile(filename);

			try (Writer writer = new FileWriter(file.toFile())) {
				getPlantUMLWriter(options).write(view, writer);
			}

			return this;

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
		private final @With DependencyDepth dependencyDepth;

		/**
		 * A {@link Predicate} to define the which modules to exclude from the diagram to be created.
		 */
		private final @With Predicate<Module> exclusions;

		/**
		 * A {@link Predicate} to define which Structurizr {@link Component}s to be included in the diagram to be created.
		 */
		private final @With Predicate<Component> componentFilter;

		/**
		 * A {@link Predicate} to define which of the modules shall only be considered targets, i.e. all efferent
		 * relationships are going to be hidden from the rendered view. Modules that have no incoming relationships will
		 * entirely be removed from the view.
		 */
		private final @With Predicate<Module> targetOnly;

		/**
		 * The target file name to be used for the diagram to be created. For individual module diagrams this needs to
		 * include a {@code %s} placeholder for the module names.
		 */
		private final @With @Nullable String targetFileName;

		/**
		 * A callback to return a hex-encoded color per {@link Module}.
		 */
		private final @With Function<Module, Optional<String>> colorSelector;

		/**
		 * A callback to return a default display names for a given {@link Module}. Default implementation just forwards to
		 * {@link Module#getDisplayName()}.
		 */
		private final @With Function<Module, String> defaultDisplayName;

		private final @With CanvasOptions canvasOptions;

		/**
		 * Creates a new default {@link Options} instance configured to use all dependency types, list immediate
		 * dependencies for individual module instances, not applying any kind of {@link Module} or {@link Component}
		 * filters and default file names.
		 *
		 * @return will never be {@literal null}.
		 */
		public static Options defaults() {
			return new Options(ALL_TYPES, DependencyDepth.IMMEDIATE, it -> false, it -> true, it -> false, null,
					__ -> Optional.empty(), it -> it.getDisplayName(), CanvasOptions.defaults());
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
					colorSelector, defaultDisplayName, canvasOptions);
		}

		private Optional<String> getTargetFileName() {
			return Optional.ofNullable(targetFileName);
		}

		private Stream<DependencyType> getDependencyTypes() {
			return dependencyTypes.stream();
		}
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class CanvasOptions {

		static final String FALLBACK_GROUP = "Others";

		private final Map<String, Predicate<SpringBean>> groupers;
		private final @With @Getter @Nullable String apiBase;

		public static CanvasOptions defaults() {

			return withoutDefaultGroupings()
					.grouping("Controllers", bean -> bean.toArchitecturallyEvidentType().isController()) //
					.grouping("Services", bean -> bean.toArchitecturallyEvidentType().isService()) //
					.grouping("Repositories", bean -> bean.toArchitecturallyEvidentType().isRepository()) //
					.grouping("Event listeners", bean -> bean.toArchitecturallyEvidentType().isEventListener());
		}

		public static CanvasOptions withoutDefaultGroupings() {
			return new CanvasOptions(new HashMap<>(), null);
		}

		public static Predicate<SpringBean> nameMatching(String pattern) {
			return bean -> bean.getFullyQualifiedTypeName().matches(pattern);
		}

		public static Predicate<SpringBean> implementing(Class<?> type) {
			return bean -> bean.getType().isAssignableTo(type);
		}

		public static Predicate<SpringBean> subtypeOf(Class<?> type) {
			return implementing(type) //
					.and(bean -> !bean.getType().isEquivalentTo(type));
		}

		public CanvasOptions grouping(String name, Predicate<SpringBean> filter) {

			Map<String, Predicate<SpringBean>> result = new HashMap<>(groupers);
			result.put(name, filter);

			return new CanvasOptions(result, apiBase);
		}

		MultiValueMap<String, SpringBean> groupBeans(Module module) {

			LinkedHashMap<String, Predicate<SpringBean>> sources = new LinkedHashMap<>(groupers);
			sources.put(FALLBACK_GROUP, it -> true);

			MultiValueMap<String, SpringBean> result = new LinkedMultiValueMap<>();
			List<SpringBean> alreadyMapped = new ArrayList<>();

			sources.forEach((key, filter) -> {

				List<SpringBean> matchingBeans = getMatchingBeans(module, filter, alreadyMapped);

				result.addAll(key, matchingBeans);
				alreadyMapped.addAll(matchingBeans);
			});

			// Wipe entries without any beans
			new HashSet<>(result.keySet()).forEach(key -> {
				if (result.get(key).isEmpty()) {
					result.remove(key);
				}
			});

			return result;
		}

		private static List<SpringBean> getMatchingBeans(Module module, Predicate<SpringBean> filter,
				List<SpringBean> alreadyMapped) {

			return module.getSpringBeans().stream()
					.filter(it -> !alreadyMapped.contains(it))
					.filter(filter::test)
					.collect(Collectors.toList());
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
		protected String backgroundOf(@Nullable View view, @Nullable Element element) {

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
