package cc.refectorie.user.kedarb.tools.opts;

import cc.refectorie.user.kedarb.tools.utils.StrUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class OptInfo {
	private static Logger logger = Logger.getLogger(OptInfo.class.getSimpleName());

	public String group, name, gloss;
	public boolean required, specified;
	public Field field;
	public Object obj;
	public String stringRepn;

	public String fullName() {
		return group + "." + name;
	}

	public String getEnumStr() {
		return getEnumStr(field.getType());
	}

	@SuppressWarnings("unchecked")
	public static String getEnumStr(Class c) {
		if (isEnum(c))
			return StrUtils.join(c.getEnumConstants(), "|");
		else
			return "";
	}

	private String typeStr() {
		return typeStr(field.getGenericType());
	}

	@SuppressWarnings("unchecked")
	private static boolean isEnum(Type type) {
		return type instanceof Class && ((Class) type).isEnum();
	}

	// Array detectors
	static boolean objIsArray(Object o) {
		return typeIsArray(o.getClass());
	}

	@SuppressWarnings("unchecked")
	static boolean typeIsArray(Type t) {
		return t instanceof Class && ((Class) t).getComponentType() != null;
	}

	@SuppressWarnings("unchecked")
	static Class arrayTypeOfObj(Object o) {
		return arrayTypeOfType(o.getClass());
	}

	@SuppressWarnings("unchecked")
	static Class arrayTypeOfType(Type t) {
		return (Class) ((Class) t).getComponentType();
	}

	private static String typeStr(Type type) {
		if (type.equals(boolean.class) || type.equals(Boolean.class))
			return "bool";
		if (type.equals(int.class) || type.equals(Integer.class))
			return "int";
		if (type.equals(short.class) || type.equals(Short.class))
			return "shrt";
		if (type.equals(double.class) || type.equals(Double.class))
			return "dbl";
		if (type.equals(String.class))
			return "str";
		if (type.equals(BufferedReader.class))
			return "read";
		if (type.equals(Random.class))
			return "rand";
		if (isEnum(type))
			return "enum";
		if (typeIsArray(type))
			return typeStr(arrayTypeOfType(type)) + "*";
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			type = ptype.getRawType();
			Type[] childTypes = ptype.getActualTypeArguments();
			if (type.equals(ArrayList.class))
				return typeStr(childTypes[0]) + "*";
		}
		return "unk";
	}

	public Object getValue() {
		try {
			return field.get(obj);
		} catch (IllegalAccessException e) {
			System.err.println("Can't access field: " + e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public String valueStr() {
		Object o = getValue();
		if (o == null)
			return "";
		if (o instanceof ArrayList)
			return StrUtils.join((ArrayList) o);

		// Array
		if (objIsArray(o)) {
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < Array.getLength(o); i++) {
				if (i > 0)
					buff.append(' ');
				buff.append(Array.get(o, i));
			}
			return buff.toString();
		}
		return o.toString();
	}

	public String toString() {
		String s = String.format("%-30s <%5s>", fullName(), typeStr());
		if (required)
			s += " *REQUIRED*";
		s += String.format(" : %s [%s]", gloss, valueStr());
		String t = getEnumStr();
		if (!t.equals(""))
			s += " " + t;
		return s;
	}

	public String toShortString() {
		return String.format("%s.%s = %s", group, name, stringRepn);
	}

	public void shortPrint() {
		logger.info("   " + this.toShortString());
	}

	public void print() {
		logger.info("   " + this.toString());
	}

	// Return errorValue if there's an error (null is a valid value).
	// type: the data type of the variable
	// l: the command line arguments to interpret
	private static String errorValue = "ERROR";

	@SuppressWarnings("unchecked")
	private static Object interpretValue(Type type, List<String> l,
			String fullName) {
		int n = l.size();
		String firstArg = n > 0 ? l.get(0) : null;

		if (type.equals(boolean.class) || type.equals(Boolean.class)) {
			boolean x = (n == 0 ? true : Boolean.parseBoolean(firstArg));
			return x;
		}
		if (type.equals(int.class) || type.equals(Integer.class)) {
			if (!checkNumArgs(1, n, fullName))
				return errorValue;
			int x;
			if (firstArg.equals("MAX"))
				x = Integer.MAX_VALUE;
			else if (firstArg.equals("MIN"))
				x = Integer.MIN_VALUE;
			else
				x = Integer.parseInt(firstArg);
			return x;
		}
		if (type.equals(short.class) || type.equals(Short.class)) {
			if (!checkNumArgs(1, n, fullName))
				return errorValue;
			short x;
			if (firstArg.equals("MAX"))
				x = Short.MAX_VALUE;
			else if (firstArg.equals("MIN"))
				x = Short.MIN_VALUE;
			else
				x = Short.parseShort(firstArg);
			return x;
		}
		if (type.equals(double.class) || type.equals(Double.class)) {
			if (!checkNumArgs(1, n, fullName))
				return errorValue;
			double x;
			if (firstArg.equals("MAX"))
				x = Double.POSITIVE_INFINITY;
			else if (firstArg.equals("MIN"))
				x = Double.NEGATIVE_INFINITY;
			else
				x = Double.parseDouble(firstArg);
			return x;
		}
		if (type.equals(int[].class)) {
			int[] x = new int[l.size()];
			for (int i = 0; i < l.size(); i++)
				x[i] = Integer.parseInt(l.get(i));
			return x;
		}
		if (type.equals(double[].class)) {
			double[] x = new double[l.size()];
			for (int i = 0; i < l.size(); i++)
				x[i] = Double.parseDouble(l.get(i));
			return x;
		}
		if (type.equals(String[].class)) {
			String[] x = new String[l.size()];
			for (int i = 0; i < l.size(); i++)
				x[i] = l.get(i);
			return x;
		}
		if (type.equals(String.class)) { // Join many arguments using spaces
			String x = StrUtils.join(l);
			return x;
		}
		if (type.equals(BufferedReader.class)) {
			if (!checkNumArgs(1, n, fullName))
				return errorValue;
			BufferedReader x = "-".equals(firstArg) ? new BufferedReader(
					new InputStreamReader(System.in)) : openIn(firstArg);
			return x;
		}
		if (type.equals(Random.class)) {
			if (!checkNumArgs(1, n, fullName))
				return errorValue;
			// seed 0 means use the time
			int seed = Integer.parseInt(firstArg);
			Random x = seed == 0 ? new Random() : new Random(seed);
			return x;
		}
		if (type instanceof Class && ((Class) type).isEnum()) {
			if (n == 0)
				return null;
			if (!checkNumArgs(1, n, fullName))
				return errorValue;
			Object x = parseEnum((Class) type, firstArg);
			if (x == null) {
				System.err.println("Invalid enum: '" + firstArg
						+ "'; valid choices: " + getEnumStr((Class) type));
				return errorValue;
			}
			return x;
		}

		// Foo[], where Foo is any class
		if (typeIsArray(type)) {
			// Put the elements in the array
			Class childType = arrayTypeOfType(type);
			Object x = Array.newInstance(childType, l.size());
			int i = 0;
			for (String a : l) {
				Object o = interpretValue(childType, Arrays.asList(a), fullName);
				if (o == errorValue)
					return errorValue;
				Array.set(x, i++, o);
			}
			return x;
		}

		// Pair or ArrayList
		if (type instanceof ParameterizedType) {
			// Types involving generics: pair, arraylist
			ParameterizedType ptype = (ParameterizedType) type;
			type = ptype.getRawType();
			Type[] childTypes = ptype.getActualTypeArguments();

			if (type.equals(List.class) || type.equals(ArrayList.class)) {
				ArrayList x = new ArrayList();
				// Put the elements in the array
				for (String a : l) {
					Object o = interpretValue(childTypes[0], Arrays.asList(a),
							fullName);
					if (o == errorValue)
						return errorValue;
					x.add(o);
				}
				return x;
			}
		}

		// Try to construct the weird type using the constructor
		// that takes one string argument.
		if (type instanceof Class) {
			try {
				Constructor con = ((Class) type).getConstructor(String.class);
				return con.newInstance(new Object[] { StrUtils.join(l) });
			} catch (Exception e) {
				System.err.println("Failed to construct " + type + ": " + e);
				e.printStackTrace();
				return errorValue;
			}
		}

		System.err.println("Can't handle weird field type: " + type);
		return errorValue;
	}

	@SuppressWarnings("unchecked")
	public static Object parseEnum(Class c, String s) {
		s = s.toLowerCase();
		for (Object o : c.getEnumConstants())
			if (o.toString().toLowerCase().equals(s))
				return o;
		return null;
	}

	public static BufferedReader openIn(String fileName) {
		try {
			return new BufferedReader(new FileReader(fileName));
		} catch (Exception e) {
			System.err.println("Couldn't open file: " + fileName);
			return null;
		}
	}

	private static boolean checkNumArgs(int want, int have, String fullName) {
		if (have != want) {
			System.err.printf(want + " arguments required for " + fullName
					+ ", but got " + have + "\n");
			return false;
		}
		return true;
	}

	private Type getGenericType() {
		return field.getGenericType();
	}

	private boolean isBool(Type type) {
		return type.equals(boolean.class) || type.equals(Boolean.class);
	}

	private void setField(Object v) throws IllegalAccessException {
		assert field != null : "Field is null for " + fullName();
		field.set(obj, v);
	}

	@SuppressWarnings("unchecked")
	public boolean set(List<String> l, boolean append) {
		try {
			Object v = interpretValue(getGenericType(), l, fullName());
			if (v == errorValue)
				return false;
			// System.out.println(name + " " + stringRepn + " " + v);
			if (!append) {
				// Treat boolean case specially because -flag means true, and l
				// is empty
				if (isBool(getGenericType()))
					stringRepn = v.toString();
				else
					stringRepn = StrUtils.join(l);
				setField(v);
			} else {
				Object oldv = field.get(obj);
				stringRepn = (stringRepn == null ? "" : stringRepn + " ")
						+ StrUtils.join(l);
				if (oldv instanceof ArrayList)
					((ArrayList) oldv).addAll((ArrayList) v);
				else if (oldv instanceof String)
					setField((oldv == null ? "" : (String) oldv + " ") + v);
			}
		} catch (IllegalAccessException e) {
			System.err.println("Can't set field: " + e);
			return false;
		}

		specified = true;
		return true;
	}
}
