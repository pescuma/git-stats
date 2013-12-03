package org.pescuma.datatable;

import static java.util.Arrays.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class MemoryDataTable implements DataTable {
	
	private final Collection<LineImpl> lines;
	
	public MemoryDataTable() {
		lines = new ArrayList<LineImpl>();
	}
	
	private MemoryDataTable(Collection<LineImpl> lines) {
		this.lines = lines;
	}
	
	@Override
	public boolean isEmpty() {
		return lines.isEmpty();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<Line> getLines() {
		return (Collection) Collections.unmodifiableCollection(lines);
	}
	
	@Override
	public Collection<String> getDistinct(int column) {
		// By default sort by the columns text
		Set<String> result = new TreeSet<String>();
		for (Line line : lines) {
			String info = line.getColumn(column);
			result.add(info);
		}
		return Collections.unmodifiableCollection(result);
	}
	
	@Override
	public Collection<String[]> getDistinct(int... columns) {
		// By default sort by the columns text
		Set<String[]> result = new TreeSet<String[]>(getLinesComparator());
		for (Line line : lines) {
			String[] info = line.getColumns(columns);
			result.add(info);
		}
		return Collections.unmodifiableCollection(result);
	}
	
	@Override
	public Map<String, Value> sumDistinct(int columns) {
		// By default sort by the columns text
		Map<String, Value> result = new TreeMap<String, Value>(String.CASE_INSENSITIVE_ORDER);
		
		for (Line line : lines) {
			String key = line.getColumn(columns);
			
			Value value = result.get(key);
			if (value == null) {
				value = new Value();
				result.put(key, value);
			}
			
			value.value += line.getValue();
		}
		
		return result;
	}
	
	@Override
	public Map<String[], Value> sumDistinct(int... columns) {
		// By default sort by the columns text
		Map<String[], Value> result = new TreeMap<String[], Value>(getLinesComparator());
		
		for (Line line : lines) {
			String[] key = line.getColumns(columns);
			
			Value value = result.get(key);
			if (value == null) {
				value = new Value();
				result.put(key, value);
			}
			
			value.value += line.getValue();
		}
		
		return result;
	}
	
	private Comparator<String[]> getLinesComparator() {
		return new Comparator<String[]>() {
			@Override
			public int compare(String[] o1, String[] o2) {
				for (int i = 0; i < o1.length; i++) {
					int comp = o1[i].compareToIgnoreCase(o2[i]);
					if (comp != 0)
						return comp;
				}
				return 0;
			}
		};
	}
	
	@Override
	public void add(double value, String... info) {
		info = cleanup(info);
		lines.add(new LineImpl(value, info));
	}
	
	@Override
	public void inc(double value, String... info) {
		Collection<LineImpl> items = getItems(info);
		if (items.isEmpty())
			add(value, info);
		else
			items.iterator().next().value += value;
	}
	
	@Override
	public void add(DataTable other) {
		for (Line line : other.getLines())
			add(line.getValue(), line.getColumns());
	}
	
	@Override
	public void inc(DataTable other) {
		for (Line line : other.getLines())
			inc(line.getValue(), line.getColumns());
	}
	
	@Override
	public double get(final String... info) {
		Collection<LineImpl> filtered = getItems(info);
		
		int size = filtered.size();
		if (size > 1)
			throw new IllegalArgumentException("More than one line has info " + Arrays.toString(info));
		if (size < 0)
			throw new IllegalArgumentException("Line not found " + Arrays.toString(info));
		
		return filtered.iterator().next().value;
	}
	
	private Collection<LineImpl> getItems(String... aInfo) {
		final String[] info = cleanup(aInfo);
		
		return Collections2.filter(lines, new Predicate<LineImpl>() {
			@Override
			public boolean apply(LineImpl input) {
				return input.info.length <= info.length && input.infoStartsWith(info);
			}
		});
	}
	
	@Override
	public DataTable filter(String... aInfo) {
		final String[] info = cleanup(aInfo);
		
		Collection<LineImpl> filtered = Collections2.filter(lines, new Predicate<LineImpl>() {
			@Override
			public boolean apply(LineImpl input) {
				return input.infoStartsWith(info);
			}
		});
		return new MemoryDataTable(filtered);
	}
	
	@Override
	public DataTable filter(final int column, final String value) {
		Collection<LineImpl> filtered = Collections2.filter(lines, new Predicate<LineImpl>() {
			@Override
			public boolean apply(LineImpl input) {
				return input.hasInfo(column, value);
			}
		});
		return new MemoryDataTable(filtered);
	}
	
	@Override
	public DataTable filter(final Predicate<Line> predicate) {
		Collection<LineImpl> filtered = Collections2.filter(lines, new Predicate<LineImpl>() {
			@Override
			public boolean apply(LineImpl input) {
				return predicate.apply(input);
			}
		});
		return new MemoryDataTable(filtered);
	}
	
	@Override
	public DataTable filter(final int column, final Predicate<String> predicate) {
		Collection<LineImpl> filtered = Collections2.filter(lines, new Predicate<LineImpl>() {
			@Override
			public boolean apply(LineImpl input) {
				return predicate.apply(input.getColumn(column));
			}
		});
		return new MemoryDataTable(filtered);
	}
	
	@Override
	public double sum() {
		double result = 0;
		for (Line line : lines)
			result += line.getValue();
		return result;
	}
	
	@Override
	public int size() {
		return lines.size();
	}
	
	@Override
	public Collection<String> getColumn(final int column) {
		return Collections2.transform(lines, new Function<Line, String>() {
			@Override
			public String apply(Line input) {
				return input.getColumn(column);
			}
		});
	}
	
	@Override
	public Collection<String[]> getColumns(final int... columns) {
		return Collections2.transform(lines, new Function<Line, String[]>() {
			@Override
			public String[] apply(Line input) {
				return input.getColumns(columns);
			}
		});
	}
	
	private String[] cleanup(String... info) {
		info = replaceNull(info);
		info = removeEmptyAtEnd(info);
		return info;
	}
	
	private String[] replaceNull(String[] info) {
		int nullPos = findNull(info);
		if (nullPos < 0)
			return info;
		
		String[] result = Arrays.copyOf(info, info.length);
		for (int i = nullPos; i < result.length; i++) {
			if (result[i] == null)
				result[i] = "";
		}
		return result;
	}
	
	private int findNull(String[] info) {
		for (int i = 0; i < info.length; i++)
			if (info[i] == null)
				return i;
		return -1;
	}
	
	private String[] removeEmptyAtEnd(String[] info) {
		int last = info.length - 1;
		for (; last > 0 && info[last].isEmpty(); last--)
			;
		last++;
		if (last == info.length)
			return info;
		else
			return copyOf(info, last);
	}
	
	private static class LineImpl implements Line {
		
		double value;
		final String[] info;
		
		LineImpl(double value, String[] info) {
			this.value = value;
			this.info = info;
		}
		
		boolean hasInfo(int column, String name) {
			if (column >= info.length)
				return name.isEmpty();
			
			return info[column].equals(name);
		}
		
		boolean infoStartsWith(String[] start) {
			for (int i = 0; i < start.length; i++) {
				String val = getColumn(i);
				if (!val.equals(start[i]))
					return false;
			}
			
			return true;
		}
		
		@Override
		public double getValue() {
			return value;
		}
		
		@Override
		public String getColumn(int column) {
			if (column < info.length)
				return info[column];
			else
				return "";
		}
		
		@Override
		public String[] getColumns(int... columns) {
			if (columns == null || columns.length == 0)
				return info;
			
			String[] result = new String[columns.length];
			for (int i = 0; i < columns.length; i++)
				result[i] = getColumn(columns[i]);
			return result;
		}
		
		@Override
		public String toString() {
			return "LineImpl [" + value + " " + Arrays.toString(info) + "]";
		}
	}
	
}
