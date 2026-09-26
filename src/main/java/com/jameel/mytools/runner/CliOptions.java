package com.jameel.mytools.runner;

import java.util.LinkedHashMap;
import java.util.Map;

final class CliOptions {

	private CliOptions() {
	}

	static Map<String, String> parse(String[] args) {
		Map<String, String> options = new LinkedHashMap<>();
		for (int i = 1; i < args.length; i++) {
			String arg = args[i];
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException("Expected option name, got: " + arg);
			}

			String name = arg.substring(2);
			if (name.isBlank()) {
				throw new IllegalArgumentException("Option name cannot be blank");
			}

			String value = "true";
			if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
				value = args[++i];
			}
			options.put(name, value);
		}
		return options;
	}
}
