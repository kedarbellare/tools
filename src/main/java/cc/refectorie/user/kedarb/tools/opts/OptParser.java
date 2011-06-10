package cc.refectorie.user.kedarb.tools.opts;

import org.apache.log4j.Logger;

import java.lang.reflect.*;
import java.util.*;

/**
 * Given a set of objects, this class first registers all the fields annotated
 * with @Opt. Next given args[] it populates the options.
 */
public class OptParser {
	private static Logger logger = Logger.getLogger(OptParser.class.getSimpleName());
	
	// each object can be a class or an object
	private HashMap<String, Object> objects = new HashMap<String, Object>();
	private ArrayList<OptInfo> options;
	private boolean ignoreUnknownOpts, mustMatchFullName, relaxRequired;

	public OptParser() {

	}

	public OptParser(Object... objects) {
		doRegisterAll(objects);
	}

	/**
	 * Registers a set of classes/objects that contain publicly accessible
	 * options
	 * 
	 * @param objects
	 *            array of class/object
	 * @return parser
	 */
	public OptParser doRegisterAll(Object... objects) {
		for (Object o : objects) {
			if (o == null)
				continue;
			doRegister(classOf(o).getSimpleName(), o);
		}
		return this;
	}

	public OptParser doRegister(String group, Object o) {
		if (objects.containsKey(group))
			throw new RuntimeException("Group name already exists: " + group);
		objects.put(group, o);
		return this;
	}

	private ArrayList<OptInfo> getOptInfos() {
		ArrayList<OptInfo> options = new ArrayList<OptInfo>();

		// for each group ..
		for (String group : objects.keySet()) {
			Object obj = objects.get(group);

			// for each field that has an option annotation ..
			for (Field field : classOf(obj).getFields()) {
				Opt ann = field.getAnnotation(Opt.class);
				if (ann == null)
					continue;

				// get the option
				OptInfo opt = new OptInfo();
				opt.group = group;
				opt.name = ann.name().equals("") ? field.getName() : ann.name();
				opt.gloss = ann.gloss();
				opt.required = ann.required();
				opt.obj = obj;
				opt.field = field;
				options.add(opt);
			}
		}

		return options;
	}

	public void printHelp() {
		printHelp(options);
	}

	private static void printHelp(List<OptInfo> options) {
		System.out.println("Usage:");
		for (OptInfo opt : options)
			opt.print();
	}

	// Return true iff x is a strict prefix of
	private static boolean isStrictPrefixOf(String x, String... ys) {
		for (String y : ys)
			if (x.startsWith(y) && x.length() > y.length())
				return true;
		return false;
	}

	private static String stripDashes(String s) {
		int i = 0;
		while (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+'))
			i++;
		return s.substring(i);
	}

	/**
	 * Returns set of options matching a given key
	 * 
	 * @param options
	 *            list of valid options
	 * @param s
	 *            option key
	 * @param allowMultipleMatches
	 *            whether to allow multiple matches
	 * @return
	 */
	private List<OptInfo> matchOpt(List<OptInfo> options, String s,
			boolean allowMultipleMatches) {
		s = s.toLowerCase();

		ArrayList<OptInfo> completeMatches = new ArrayList<OptInfo>();
		ArrayList<OptInfo> partialMatches = new ArrayList<OptInfo>();
		for (OptInfo opt : options) {
			String t;
			// first type to match full name
			t = opt.fullName().toLowerCase();
			if (t.equals(s))
				completeMatches.add(opt);
			if (t.startsWith(s))
				partialMatches.add(opt);

			// otherwise match name (without the group)
			if (!mustMatchFullName) {
				t = opt.name.toLowerCase();
				if (t.equals(s))
					completeMatches.add(opt);
				if (t.startsWith(s))
					partialMatches.add(opt);
			}
		}

		if (completeMatches.size() == 0 && partialMatches.size() == 0) {
			if (!ignoreUnknownOpts)
				System.err.println("Unknown option: '" + s
						+ "'; -help for usage");
			return completeMatches;
		}

		if (allowMultipleMatches)
			return partialMatches;
		else {
			// enforce one match
			if (completeMatches.size() == 1)
				return completeMatches;
			if (completeMatches.size() == 0 && partialMatches.size() == 1)
				return partialMatches;

			System.err.println("Ambiguous option: '" + s
					+ "'; possible matches:");
			for (OptInfo opt : partialMatches)
				opt.print();
			return new ArrayList<OptInfo>();
		}
	}

	public OptParser ignoreUnknownOpts() {
		this.ignoreUnknownOpts = true;
		return this;
	}

	public OptParser mustMatchFullName() {
		this.mustMatchFullName = true;
		return this;
	}

	public OptParser relaxRequired() {
		this.relaxRequired = true;
		return this;
	}

	public boolean doParse(String[] args) {
		if (this.options == null)
			this.options = getOptInfos();

		// parse the arguments
		for (int i = 0; i < args.length;) {
			if (args[i].equals("-help")) {
				// get usage help
				printHelp(options);
				i++;
				return false;
			} else if (isStrictPrefixOf(args[i], "-", "+", "--")) {
				boolean append = args[i].startsWith("+");
				boolean allowMultipleMatches = args[i].startsWith("--");
				List<OptInfo> opts = matchOpt(options, stripDashes(args[i++]),
						allowMultipleMatches);

				// get the data values of this parameter
				ArrayList<String> l = new ArrayList<String>();
				boolean nextIsVerbatim = false;
				boolean allIsVerbatim = false;
				while (i < args.length) {
					if (args[i].equals("--"))
						nextIsVerbatim = true;
					else if (args[i].equals("---"))
						allIsVerbatim = !allIsVerbatim;
					else {
						if (!allIsVerbatim && !nextIsVerbatim
								&& (isStrictPrefixOf(args[i], "+", "-")))
							break;
						l.add(args[i]);
						nextIsVerbatim = false;
					}
					i++;
				}

				// pass data values to matched options
				if (opts.size() == 0 && !ignoreUnknownOpts)
					return false;
				for (OptInfo opt : opts) {
					if (!opt.set(l, append)) {
						if (ignoreUnknownOpts)
							continue;
						else
							return false;
					}
				}
			} else {
				System.err
						.println("Argument not part of an option: " + args[i]);
				if (!ignoreUnknownOpts)
					return false;
			}
		}

		// check that all required options are specified
		if (!relaxRequired) {
			List<String> missingOptMsgs = new ArrayList<String>();
			for (OptInfo o : options) {
				String msg = isMissing(o, options);
				if (msg != null)
					missingOptMsgs.add(msg);
			}
			if (missingOptMsgs.size() > 0) {
				System.err.println("Missing required option(s):");
				for (String msg : missingOptMsgs)
					System.err.println(msg);
				return false;
			}
		}

		// print specified options
		logger.info("Specified options: ");
		for (OptInfo opt : options)
			if (opt.specified)
				opt.shortPrint();

		return true;
	}

	private String isMissing(OptInfo o, List<OptInfo> options) {
		if (o.specified)
			return null; // specified, we're fine
		if (o.required)
			return o.toString(); // this option is required
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Class classOf(Object o) {
		return (o instanceof Class) ? (Class) o : o.getClass();
	}
}
