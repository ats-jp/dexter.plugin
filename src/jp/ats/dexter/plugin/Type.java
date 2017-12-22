package jp.ats.dexter.plugin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jp.ats.substrate.U;

enum Type {

	String("type-string", String.class),

	Int("type-int", int.class),

	Long("type-long", long.class),

	Boolean("type-boolean", boolean.class),

	Decimal("type-decimal", BigDecimal.class),

	Date("type-date", LocalDate.class),

	Time("type-time", LocalTime.class);

	final String expression;

	final Class<?> clazz;

	private Type(String expression, Class<?> clazz) {
		this.expression = expression;
		this.clazz = clazz;
	}

	String getWrapperName() {
		return U.convertPrimitiveClassToWrapperClass(clazz).getSimpleName();
	}

	String getConverterName() {
		return "to" + name();
	}

	boolean needsImport() {
		return !clazz.isPrimitive()
			&& !Object.class.getPackage().equals(clazz.getPackage());
	}

	String getImportString() {
		return "import " + clazz.getName() + ";";
	}
}
