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

import org.moduliths.model.FormatableJavaClass;
import org.moduliths.model.Module;
import org.moduliths.model.SpringBean;
import org.springframework.lang.Nullable;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(staticName = "withJavadocBase")
class Asciidoctor {

	private final String javaDocBase;
	private final @Nullable Module module;

	public static Asciidoctor withoutJavadocBase(@Nullable Module module) {
		return new Asciidoctor("¯\\_(ツ)_/¯", module);
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

		String base = toInlineCode(bean.getType());

		List<JavaClass> interfaces = bean.getInterfacesWithinModule();

		if (interfaces.isEmpty()) {
			return base;
		}

		String interfacesAsString = interfaces.stream() //
				.map(this::toInlineCode) //
				.collect(Collectors.joining(", "));

		return String.format("%s implementing %s", base, interfacesAsString);
	}

	public String beansToBulletPoints(List<SpringBean> beans) {
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

		if (!source.getModifiers().contains(JavaModifier.PUBLIC)) {
			return type;
		}

		String classPath = convertClassNameToResourcePath(source.getFullName()) //
				.replace('$', '.');

		return Optional.ofNullable(javaDocBase == "¯\\_(ツ)_/¯" ? null : javaDocBase) //
				.map(it -> it.concat("/").concat(classPath).concat(".html")) //
				.map(it -> toLink(type, it)) //
				.orElse(type);
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
