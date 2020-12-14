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
package org.moduliths.docs;

import static org.springframework.util.ClassUtils.*;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.moduliths.docs.Documenter.CanvasOptions;
import org.moduliths.docs.Documenter.CanvasOptions.Groupings;
import org.moduliths.model.ArchitecturallyEvidentType;
import org.moduliths.model.EventType;
import org.moduliths.model.FormatableJavaClass;
import org.moduliths.model.Module;
import org.moduliths.model.Source;
import org.moduliths.model.SpringBean;
import org.springframework.util.ClassUtils;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(staticName = "withJavadocBase")
class Asciidoctor {

	private static String PLACEHOLDER = "¯\\_(ツ)_/¯";
	private static final DocumentationSource JAVADOC_SOURCE;

	private final String javaDocBase;
	private final Module module;

	static {

		JAVADOC_SOURCE = ClassUtils.isPresent("capital.scalable.restdocs.javadoc.JavadocReaderImpl",
				Asciidoctor.class.getClassLoader())
						? new SpringAutoRestDocsDocumentationSource()
						: null;
	}

	public static Asciidoctor withoutJavadocBase(Module module) {
		return new Asciidoctor(PLACEHOLDER, module);
	}

	/**
	 * Turns the given source string into inline code.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	public String toInlineCode(String source) {
		return String.format("`%s`", source);
	}

	public String toInlineCode(JavaClass type) {
		return toOptionalLink(type);
	}

	public String toInlineCode(SpringBean bean) {

		String base = toInlineCode(bean.toArchitecturallyEvidentType());

		List<JavaClass> interfaces = bean.getInterfacesWithinModule();

		if (interfaces.isEmpty()) {
			return base;
		}

		String interfacesAsString = interfaces.stream() //
				.map(this::toInlineCode) //
				.collect(Collectors.joining(", "));

		return String.format("%s implementing %s", base, interfacesAsString);
	}

	public String renderSpringBeans(CanvasOptions options) {

		StringBuilder builder = new StringBuilder();
		Groupings groupings = options.groupBeans(module);

		if (groupings.hasOnlyFallbackGroup()) {
			return toBulletPoints(groupings.byGrouping(CanvasOptions.FALLBACK_GROUP));
		}

		groupings.forEach((grouping, beans) -> {

			if (beans.isEmpty()) {
				return;
			}

			if (builder.length() != 0) {
				builder.append("\n\n");
			}

			builder.append("_").append(grouping.getName()).append("_");

			if (grouping.getDescription() != null) {
				builder.append(" -- ").append(grouping.getDescription());
			}

			builder.append("\n\n");
			builder.append(toBulletPoints(beans));

		});

		return builder.length() == 0 ? "None" : builder.toString();
	}

	public String renderEvents(List<EventType> events) {

		if (events.isEmpty()) {
			return "none";
		}

		StringBuilder builder = new StringBuilder();

		for (EventType eventType : events) {

			builder.append("* ")
					.append(toInlineCode(eventType.getType()));

			if (!eventType.hasSources()) {
				builder.append("\n");
			} else {
				builder.append(" created by:\n");
			}

			for (Source source : eventType.getSources()) {

				builder.append("** ")
						.append(toInlineCode(source.toString(module)))
						.append("\n");
			}
		}

		return builder.toString();
	}

	private String toBulletPoints(List<SpringBean> beans) {
		return toBulletPoints(beans.stream().map(this::toInlineCode));
	}

	public String typesToBulletPoints(List<JavaClass> types) {
		return toBulletPoints(types.stream() //
				.map(this::toOptionalLink));
	}

	private String toBulletPoints(Stream<String> types) {

		return types//
				.collect(Collectors.joining("\n* ", "* ", ""));
	}

	public String toBulletPoint(String source) {
		return String.format("* %s", source);
	}

	private String toOptionalLink(JavaClass source) {

		String type = toCode(FormatableJavaClass.of(source).getAbbreviatedFullName(module));

		if (!source.getModifiers().contains(JavaModifier.PUBLIC) ||
				!module.contains(source)) {
			return type;
		}

		String classPath = convertClassNameToResourcePath(source.getFullName()) //
				.replace('$', '.');

		return Optional.ofNullable(javaDocBase == PLACEHOLDER ? null : javaDocBase) //
				.map(it -> it.concat("/").concat(classPath).concat(".html")) //
				.map(it -> toLink(type, it)) //
				.orElse(type);
	}

	private String toInlineCode(ArchitecturallyEvidentType type) {

		if (type.isEventListener()) {

			if (JAVADOC_SOURCE == null) {

				return String.format("%s listening to %s", //
						toInlineCode(type.getType()), //
						toInlineCode(type.getReferenceTypes()));
			}

			String header = String.format("%s listening to:\n", toInlineCode(type.getType()));

			return header + type.getReferenceMethods().map(it -> {

				JavaClass parameterType = it.getMethod().getRawParameterTypes().get(0);
				String isAsync = it.isAsync() ? "(async) " : "";

				return JAVADOC_SOURCE.getDocumentation(it.getMethod())
						.map(doc -> String.format("** %s %s-- %s", toInlineCode(parameterType), isAsync, doc))
						.orElseGet(() -> String.format("** %s %s", toInlineCode(parameterType), isAsync));

			}).collect(Collectors.joining("\n"));
		}

		return toInlineCode(type.getType());
	}

	private String toInlineCode(Stream<JavaClass> types) {

		return types.map(this::toInlineCode) //
				.collect(Collectors.joining(", "));
	}

	private static String toLink(String source, String href) {
		return String.format("link:%s[%s]", href, source);
	}

	private static String toCode(String source) {
		return String.format("`%s`", source);
	}

	public static String startTable(String tableSpec) {
		return String.format("[%s]\n|===\n", tableSpec);
	}

	public static String startOrEndTable() {
		return "|===\n";
	}

	public static String writeTableRow(String... columns) {

		return Stream.of(columns) //
				.collect(Collectors.joining("\n|", "|", "\n"));
	}
}
