package com.jameel.mytools.conversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class CsvSupport {

	private CsvSupport() {
	}

	static List<String> readRecord(BufferedReader reader) throws IOException {
		List<String> fields = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean inQuotes = false;
		boolean sawAny = false;

		while (true) {
			int next = reader.read();
			if (next == -1) {
				if (!sawAny && field.isEmpty() && fields.isEmpty()) {
					return null;
				}
				fields.add(field.toString());
				return fields;
			}

			sawAny = true;
			char ch = (char) next;
			if (inQuotes) {
				if (ch == '"') {
					reader.mark(1);
					int escaped = reader.read();
					if (escaped == '"') {
						field.append('"');
					} else {
						inQuotes = false;
						if (escaped != -1) {
							reader.reset();
						}
					}
				} else {
					field.append(ch);
				}
				continue;
			}

			if (ch == '"') {
				inQuotes = true;
			} else if (ch == ',') {
				fields.add(field.toString());
				field.setLength(0);
			} else if (ch == '\n') {
				fields.add(field.toString());
				return fields;
			} else if (ch != '\r') {
				field.append(ch);
			}
		}
	}

	static void writeRecord(BufferedWriter writer, List<String> fields) throws IOException {
		for (int i = 0; i < fields.size(); i++) {
			if (i > 0) {
				writer.write(',');
			}
			writer.write(escape(fields.get(i)));
		}
		writer.newLine();
	}

	private static String escape(String value) {
		String text = value == null ? "" : value;
		if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
			return "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}
}
