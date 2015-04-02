package org.pescuma.datatable;

import static java.lang.System.*;
import static java.util.Arrays.*;
import static org.apache.commons.io.FileUtils.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.pescuma.datatable.DataTable.Line;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.io.Closer;

public class DataTableSerialization {
	
	public static void saveAsCSV(DataTable data, File file, boolean append) {
		Closer closer = Closer.create();
		try {
			try {
				forceMkdir(file.getParentFile());
				
				FileWriter writer = closer.register(new FileWriter(file, append));
				CSVWriter csv = closer.register(newCSVWriter(writer));
				
				for (Line line : data.getLines()) {
					String[] cols = line.getColumns();
					
					String[] toWrite = new String[cols.length + 1];
					toWrite[0] = Double.toString(line.getValue());
					arraycopy(cols, 0, toWrite, 1, cols.length);
					
					csv.writeNext(toWrite);
				}
				
			} catch (IOException e) {
				throw closer.rethrow(e);
				
			} finally {
				closer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error writing csv: " + file, e);
		}
	}
	
	public static void loadFromCSV(DataTable data, File file) {
		Closer closer = Closer.create();
		try {
			try {
				
				FileReader reader = closer.register(new FileReader(file));
				CSVReader csv = closer.register(newCSVReader(reader));
				
				String[] line;
				while ((line = csv.readNext()) != null) {
					double val = Double.parseDouble(line[0]);
					String[] info = copyOfRange(line, 1, line.length);
					data.add(val, info);
				}
				
			} catch (IOException e) {
				throw closer.rethrow(e);
				
			} finally {
				closer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error reading csv: " + file, e);
		}
	}
	
	private static CSVWriter newCSVWriter(Writer writer) {
		return new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR,
				CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER);
	}
	
	private static CSVReader newCSVReader(Reader reader) {
		return new CSVReader(reader, CSVWriter.DEFAULT_SEPARATOR,
				CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVParser.NULL_CHARACTER);
	}
	
}
