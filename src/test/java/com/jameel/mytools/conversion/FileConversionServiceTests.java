package com.jameel.mytools.conversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileConversionServiceTests {

	private final FileConversionService service = new FileConversionService();

	Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		Files.createDirectories(Path.of("target", "test-work"));
		tempDir = Files.createTempDirectory(Path.of("target", "test-work"), "conversion-");
	}

	@AfterEach
	void tearDown() throws IOException {
		if (tempDir != null && Files.exists(tempDir)) {
			try (var paths = Files.walk(tempDir)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		}
	}

	@Test
	void convertsCsvToXlsxAndBackWithQuotedFields() throws Exception {
		Path csv = tempDir.resolve("input.csv");
		Path xlsx = tempDir.resolve("output.xlsx");
		Path roundTrip = tempDir.resolve("round-trip.csv");
		Files.writeString(csv, "Name,Note\nAlice,\"hello, world\"\nBob,\"said \"\"yes\"\"\"\n", StandardCharsets.UTF_8);

		service.csvToXlsx(csv, xlsx, "Data");
		service.xlsxToCsv(xlsx, roundTrip, "Data");

		assertThat(Files.readAllLines(roundTrip)).containsExactly(
				"Name,Note",
				"Alice,\"hello, world\"",
				"Bob,\"said \"\"yes\"\"\"");
	}

	@Test
	void formatsDateColumnUsingHeaderName() throws Exception {
		Path input = tempDir.resolve("dates.xlsx");
		Path output = tempDir.resolve("formatted.xlsx");
		createDateWorkbook(input);

		int changed = service.formatXlsxDateColumn(input, output, "0", "Date", "dd/MM/yyyy", "yyyy-MM-dd", true);

		assertThat(changed).isEqualTo(2);
		try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(output))) {
			Sheet sheet = workbook.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			assertThat(formatter.formatCellValue(sheet.getRow(1).getCell(0))).isEqualTo("2026-09-26");
			assertThat(formatter.formatCellValue(sheet.getRow(2).getCell(0))).isEqualTo("2026-10-01");
		}
	}

	@Test
	void readsMultilineCsvRecords() throws Exception {
		Path csv = tempDir.resolve("multiline.csv");
		Files.writeString(csv, "A,B\n1,\"two\nlines\"\n", StandardCharsets.UTF_8);

		try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
			assertThat(CsvSupport.readRecord(reader)).isEqualTo(List.of("A", "B"));
			assertThat(CsvSupport.readRecord(reader)).isEqualTo(List.of("1", "two\nlines"));
			assertThat(CsvSupport.readRecord(reader)).isNull();
		}
	}

	private static void createDateWorkbook(Path path) throws Exception {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Sheet1");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Date");
			header.createCell(1).setCellValue("Amount");
			Row first = sheet.createRow(1);
			first.createCell(0).setCellValue("26/09/2026");
			first.createCell(1).setCellValue("10");
			Row second = sheet.createRow(2);
			second.createCell(0).setCellValue("01/10/2026");
			second.createCell(1).setCellValue("15");
			try (var output = Files.newOutputStream(path)) {
				workbook.write(output);
			}
		}
	}
}
