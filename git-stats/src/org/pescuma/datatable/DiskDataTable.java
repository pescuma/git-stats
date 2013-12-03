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
import java.util.Collection;
import java.util.Map;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.base.Predicate;
import com.google.common.io.Closer;

public class DiskDataTable implements DataTable {
	
	private final File file;
	private final DataTable data;
	private boolean loadedFromDisk = false;
	private boolean wroteData = false;
	
	public DiskDataTable(File file, DataTable data) {
		this.file = file;
		this.data = data;
	}
	
	public void saveToDisk() {
		if (!wroteData)
			return;
		
		Closer closer = Closer.create();
		try {
			try {
				forceMkdir(file.getParentFile());
				
				FileWriter writer = closer.register(new FileWriter(file, !loadedFromDisk));
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
	
	private void loadFromDisk() {
		if (loadedFromDisk)
			return;
		loadedFromDisk = true;
		
		if (!file.exists())
			return;
		
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
	
	@Override
	public boolean isEmpty() {
		loadFromDisk();
		return data.isEmpty();
	}
	
	@Override
	public void add(double value, String... info) {
		wroteData = true;
		data.add(value, info);
	}
	
	@Override
	public void inc(double value, String... info) {
		loadFromDisk();
		wroteData = true;
		data.inc(value, info);
	}
	
	@Override
	public void add(DataTable other) {
		if (other.isEmpty())
			return;
		
		wroteData = true;
		data.add(other);
	}
	
	@Override
	public void inc(DataTable other) {
		if (other.isEmpty())
			return;
		
		loadFromDisk();
		wroteData = true;
		data.inc(other);
	}
	
	@Override
	public double get(String... info) {
		loadFromDisk();
		return data.get(info);
	}
	
	@Override
	public Collection<Line> getLines() {
		loadFromDisk();
		return data.getLines();
	}
	
	@Override
	public Collection<String> getDistinct(int column) {
		loadFromDisk();
		return data.getDistinct(column);
	}
	
	@Override
	public Collection<String[]> getDistinct(int... columns) {
		loadFromDisk();
		return data.getDistinct(columns);
	}
	
	@Override
	public Map<String, Value> sumDistinct(int columns) {
		loadFromDisk();
		return data.sumDistinct(columns);
	}
	
	@Override
	public Map<String[], Value> sumDistinct(int... columns) {
		loadFromDisk();
		return data.sumDistinct(columns);
	}
	
	@Override
	public DataTable filter(String... info) {
		loadFromDisk();
		return data.filter(info);
	}
	
	@Override
	public DataTable filter(int column, String value) {
		loadFromDisk();
		return data.filter(column, value);
	}
	
	@Override
	public DataTable filter(Predicate<Line> predicate) {
		loadFromDisk();
		return data.filter(predicate);
	}
	
	@Override
	public DataTable filter(int column, Predicate<String> predicate) {
		loadFromDisk();
		return data.filter(column, predicate);
	}
	
	@Override
	public double sum() {
		loadFromDisk();
		return data.sum();
	}
	
	@Override
	public int size() {
		loadFromDisk();
		return data.size();
	}
	
	@Override
	public Collection<String> getColumn(int column) {
		loadFromDisk();
		return data.getColumn(column);
	}
	
	@Override
	public Collection<String[]> getColumns(int... columns) {
		loadFromDisk();
		return data.getColumns(columns);
	}
	
	private static CSVWriter newCSVWriter(Writer writer) {
		return new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER);
	}
	
	private static CSVReader newCSVReader(Reader reader) {
		return new CSVReader(reader, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER,
				CSVParser.NULL_CHARACTER);
	}
	
}
