package com.jameel.mytools.conversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class FileConversionService {

	public void csvToXlsx(Path input, Path output, String sheetName) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
				Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet(safeSheetName(sheetName));
			List<String> record;
			int rowIndex = 0;
			while ((record = CsvSupport.readRecord(reader)) != null) {
				Row row = sheet.createRow(rowIndex++);
				for (int i = 0; i < record.size(); i++) {
					row.createCell(i).setCellValue(record.get(i));
				}
			}
			autoSizeColumns(sheet);
			writeWorkbook(workbook, output);
		}
	}

	public void xlsxToCsv(Path input, Path output, String sheetSelector) throws IOException {
		try (InputStream inputStream = Files.newInputStream(input);
				Workbook workbook = new XSSFWorkbook(inputStream);
				BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			Sheet sheet = selectSheet(workbook, sheetSelector);
			DataFormatter formatter = new DataFormatter();
			for (Row row : sheet) {
				int lastCell = row.getLastCellNum();
				List<String> fields = new ArrayList<>();
				for (int i = 0; i < Math.max(lastCell, 0); i++) {
					Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					fields.add(cell == null ? "" : formatter.formatCellValue(cell));
				}
				CsvSupport.writeRecord(writer, fields);
			}
		}
	}

	public int formatXlsxDateColumn(
			Path input,
			Path output,
			String sheetSelector,
			String columnSelector,
			String fromPattern,
			String toPattern,
			boolean hasHeader) throws IOException {
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(fromPattern);
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(toPattern);
		DataFormatter dataFormatter = new DataFormatter();

		try (InputStream inputStream = Files.newInputStream(input);
				Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = selectSheet(workbook, sheetSelector);
			int columnIndex = resolveColumnIndex(sheet, columnSelector, hasHeader);
			int firstDataRow = hasHeader ? 1 : 0;
			int formattedCount = 0;

			for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}
				Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (cell == null) {
					continue;
				}

				LocalDate date = readDate(cell, dataFormatter, inputFormatter);
				if (date == null) {
					continue;
				}

				cell.setCellValue(outputFormatter.format(date));
				formattedCount++;
			}

			writeWorkbook(workbook, output);
			return formattedCount;
		}
	}

	private static LocalDate readDate(Cell cell, DataFormatter formatter, DateTimeFormatter inputFormatter) {
		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			return cell.getLocalDateTimeCellValue().toLocalDate();
		}

		String value = formatter.formatCellValue(cell).trim();
		if (value.isEmpty()) {
			return null;
		}
		try {
			return LocalDate.parse(value, inputFormatter);
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	private static int resolveColumnIndex(Sheet sheet, String selector, boolean hasHeader) {
		String trimmed = selector.trim();
		if (trimmed.matches("\\d+")) {
			int oneBased = Integer.parseInt(trimmed);
			if (oneBased < 1) {
				throw new IllegalArgumentException("Column number must be 1 or greater");
			}
			return oneBased - 1;
		}

		if (hasHeader) {
			Row header = sheet.getRow(0);
			if (header == null) {
				throw new IllegalArgumentException("Cannot find header row");
			}
			DataFormatter formatter = new DataFormatter();
			for (Cell cell : header) {
				if (trimmed.equalsIgnoreCase(formatter.formatCellValue(cell).trim())) {
					return cell.getColumnIndex();
				}
			}
		}

		if (trimmed.matches("[A-Za-z]+")) {
			return columnLettersToIndex(trimmed);
		}
		throw new IllegalArgumentException("Cannot find column header: " + selector);
	}

	private static int columnLettersToIndex(String letters) {
		int result = 0;
		for (char ch : letters.toUpperCase().toCharArray()) {
			result = result * 26 + (ch - 'A' + 1);
		}
		return result - 1;
	}

	private static Sheet selectSheet(Workbook workbook, String selector) {
		if (selector.matches("\\d+")) {
			int index = Integer.parseInt(selector);
			Sheet sheet = workbook.getSheetAt(index);
			if (sheet == null) {
				throw new IllegalArgumentException("Cannot find sheet index: " + selector);
			}
			return sheet;
		}
		Sheet sheet = workbook.getSheet(selector);
		if (sheet == null) {
			throw new IllegalArgumentException("Cannot find sheet: " + selector);
		}
		return sheet;
	}

	private static void autoSizeColumns(Sheet sheet) {
		int maxColumns = 0;
		for (Row row : sheet) {
			maxColumns = Math.max(maxColumns, row.getLastCellNum());
		}
		for (int i = 0; i < maxColumns; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private static String safeSheetName(String sheetName) {
		String value = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName.trim();
		return value.length() > 31 ? value.substring(0, 31) : value;
	}

	private static void writeWorkbook(Workbook workbook, Path output) throws IOException {
		Path parent = output.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		try (OutputStream outputStream = Files.newOutputStream(output)) {
			workbook.write(outputStream);
		}
	}
}
