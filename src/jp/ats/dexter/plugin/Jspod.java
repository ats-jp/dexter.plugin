package jp.ats.dexter.plugin;

import static jp.ats.dexter.plugin.Common.select;
import static jp.ats.substrate.U.isAvailable;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Jspod {

	final String className;

	final boolean isIdAvailable;

	private final String id;

	private final Map<String, Property> map = new LinkedHashMap<>();

	Jspod(
		String id,
		String className,
		String[] availableProperties,
		boolean isIdAvailable) {
		this.id = id;
		this.className = className;
		this.isIdAvailable = isIdAvailable;

		for (String property : availableProperties) {
			map.put(property, new ClassField(property));
		}
	}

	String processProperty(String prefixer, String content) {
		content = processProperty(
			"name",
			prefixer,
			content,
			new PropertyFactory() {

				@Override
				public Property createProperty(String name, String index) {
					return new NameProperty(name, index);
				}
			});

		content = processProperty(
			"value",
			prefixer,
			content,
			new PropertyFactory() {

				@Override
				public Property createProperty(String name, String index) {
					return new ValueProperty(name, index, id);
				}
			});

		for (Type type : Type.values()) {
			content = processType(type, type.expression, prefixer, content);
		}

		return content;
	}

	Property[] getProperties() {
		return map.values().toArray(new Property[map.size()]);
	}

	String processFormId(String prefixer, String content) {
		String smallPattern;
		if (!isIdAvailable) {
			smallPattern = prefixer + "\\.formId\\{\\}";
		} else {
			smallPattern = prefixer + "\\.formId\\{" + id + "\\}";
		}

		Matcher matcher = Pattern
			.compile(
				"<%= */\\* " + smallPattern + " \\*/[^%]+%>|" + smallPattern)
			.matcher(content);

		StringBuilder converted = new StringBuilder();

		int previous = 0;
		while (matcher.find()) {
			converted.append(content.substring(previous, matcher.start()));
			previous = matcher.end();

			converted.append(
				"<%= /* jspod.formId{"
					+ (isIdAvailable ? id : "")
					+ "} */ "
					+ className
					+ ".class.getSimpleName() %>");
		}

		converted.append(content.substring(previous));

		return converted.toString();
	}

	String processTokenTag(String prefixer, String content) {
		String smallPattern;
		if (!isIdAvailable) {
			smallPattern = prefixer + "\\.tokenTag\\{\\}";
		} else {
			smallPattern = prefixer + "\\.tokenTag\\{" + id + "\\}";
		}

		Matcher matcher = Pattern
			.compile(
				"<!-- *" + smallPattern + " *-->[^@]+@ -->|" + smallPattern)
			.matcher(content);

		StringBuilder converted = new StringBuilder();

		int previous = 0;
		while (matcher.find()) {
			converted.append(content.substring(previous, matcher.start()));
			previous = matcher.end();

			String tagId = isIdAvailable
				? id + "-" + prefixer + "-token"
				: prefixer + "-token";

			converted.append(
				"<!-- jspod.tokenTag{"
					+ (isIdAvailable ? id : "")
					+ "} --><input type=\"hidden\" id=\""
					+ tagId
					+ "\" name=\"<%="
					+ id
					+ ".tokenName()%>\" value=\"<%="
					+ id
					+ ".tokenValue()%>\" /><!-- tokenTagEnd@ -->");
		}

		converted.append(content.substring(previous));

		return converted.toString();
	}

	private String processProperty(
		String context,
		String prefixer,
		String content,
		PropertyFactory factory) {
		StringBuilder converted = new StringBuilder();

		Matcher matcher = createPropertyPattern(prefixer, context)
			.matcher(content);
		int previous = 0;
		while (matcher.find()) {
			converted.append(content.substring(previous, matcher.start()));
			previous = matcher.end();

			Property property;
			try {
				property = factory.createProperty(
					select(matcher.group(2), matcher.group(5)),
					select(matcher.group(3), matcher.group(6))
						.replaceAll("^\\[|\\]$", ""));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}

			map.put(property.name, property);

			converted.append(
				property.execute(select(matcher.group(1), matcher.group(4))));
		}

		converted.append(content.substring(previous));

		return converted.toString();
	}

	private Pattern createPropertyPattern(String prefixer, String context) {
		String smallPattern;
		if (!isIdAvailable) {
			smallPattern = prefixer
				+ "\\."
				+ context
				+ "\\{([^\\}\\[:]+)(\\[[^\\]]+\\]|)\\}";
		} else {
			smallPattern = prefixer
				+ "\\."
				+ context
				+ "\\{"
				+ id
				+ ":([^\\}\\[]+)(\\[[^\\]]+\\]|)\\}";
		}

		return Pattern.compile(
			"<%= */\\* ("
				+ smallPattern
				+ ") \\*/[^%]+%>|("
				+ smallPattern
				+ ")");
	}

	private Pattern createTypePattern(String prefixer, String context) {
		String smallPattern;
		if (!isIdAvailable) {
			smallPattern = prefixer + "\\." + context + "\\{([^\\}\\[:]+)\\}";
		} else {
			smallPattern = prefixer
				+ "\\."
				+ context
				+ "\\{"
				+ id
				+ ":([^\\}\\[]+)\\}";
		}

		return Pattern
			.compile("<!-- (" + smallPattern + ") -->|(" + smallPattern + ")");
	}

	static interface PropertyFactory {

		Property createProperty(String name, String index);
	}

	private String processType(
		Type type,
		String context,
		String prefixer,
		String content) {
		StringBuilder converted = new StringBuilder();

		Matcher matcher = createTypePattern(prefixer, context).matcher(content);
		int previous = 0;
		while (matcher.find()) {
			converted.append(content.substring(previous, matcher.start()));
			previous = matcher.end();

			String name = select(matcher.group(2), matcher.group(4));

			Property property = map.get(name);
			if (property != null) property.type = type;

			converted.append(
				MessageFormat.format(
					"<!-- {0} -->",
					select(matcher.group(1), matcher.group(3))));
		}

		converted.append(content.substring(previous));

		return converted.toString();
	}

	static abstract class Property {

		final String name;

		final String index;

		Type type = Type.String;

		Property(String name, String index) {
			this.name = name;
			this.index = index;
		}

		abstract String execute(String expression);
	}

	static class ClassField extends Property {

		private ClassField(String name) {
			super(name, null);
		}

		@Override
		String execute(String expression) {
			throw new UnsupportedOperationException();
		}
	}

	static class NameProperty extends Property {

		private NameProperty(String name, String index) {
			super(name, index);
		}

		@Override
		String execute(String expression) {
			String template;
			if (isAvailable(index)) {
				template = "<%= /* {0} */ \"{1}[\" + " + index + " + \"]\" %>";
			} else {
				template = "<%= /* {0} */ \"{1}\" %>";
			}

			return MessageFormat.format(template, expression, name);
		}
	}

	static class ValueProperty extends Property {

		private final String id;

		private ValueProperty(String name, String index, String id) {
			super(name, index);
			this.id = id;
		}

		@Override
		String execute(String expression) {
			String template;
			if (isAvailable(index)) {
				template = "<%= /* {0} */ {1}Safely(" + index + ") %>";
			} else {
				template = "<%= /* {0} */ {1}Safely() %>";
			}

			return MessageFormat
				.format(template, expression, id + ".get" + capitalize(name));
		}
	}

	static String capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}
}
