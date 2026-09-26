package com.jameel.mytools.runner;

import com.jameel.mytools.conversion.FileConversionService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ToolRunner implements ApplicationRunner {

	private final FileConversionService conversionService;

	public ToolRunner(FileConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (args.getSourceArgs().length == 0 || args.containsOption("help")) {
			printHelp();
			return;
		}

		String command = args.getSourceArgs()[0];
		Map<String, String> options = CliOptions.parse(args.getSourceArgs());

		switch (command) {
			case "csv-to-xlsx" -> conversionService.csvToXlsx(
					requiredPath(options, "input"),
					requiredPath(options, "output"),
					options.getOrDefault("sheet", "Sheet1"));
			case "xlsx-to-csv" -> conversionService.xlsxToCsv(
					requiredPath(options, "input"),
					requiredPath(options, "output"),
					options.getOrDefault("sheet", "0"));
			case "format-xlsx-date" -> conversionService.formatXlsxDateColumn(
					requiredPath(options, "input"),
					requiredPath(options, "output"),
					options.getOrDefault("sheet", "0"),
					required(options, "column"),
					required(options, "from"),
					required(options, "to"),
					Boolean.parseBoolean(options.getOrDefault("has-header", "true")));
			default -> {
				System.err.println("Unknown command: " + command);
				printHelp();
				throw new IllegalArgumentException("Unknown command: " + command);
			}
		}
	}

	private static String required(Map<String, String> options, String name) {
		String value = options.get(name);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing required option --" + name);
		}
		return value;
	}

	private static Path requiredPath(Map<String, String> options, String name) {
		return Path.of(required(options, name));
	}

	private static void printHelp() {
		System.out.println("""
				MyTools offline file conversion runner

				Commands:
				  csv-to-xlsx --input <file.csv> --output <file.xlsx> [--sheet Sheet1]
				  xlsx-to-csv --input <file.xlsx> --output <file.csv> [--sheet 0|Sheet1]
				  format-xlsx-date --input <in.xlsx> --output <out.xlsx> --column <A|1|Header> --from <pattern> --to <pattern> [--sheet 0|Sheet1] [--has-header true|false]

				Examples:
				  csv-to-xlsx --input data.csv --output data.xlsx
				  xlsx-to-csv --input data.xlsx --output data.csv --sheet Sheet1
				  format-xlsx-date --input data.xlsx --output data-formatted.xlsx --column Date --from dd/MM/yyyy --to yyyy-MM-dd
				""");
	}
}
