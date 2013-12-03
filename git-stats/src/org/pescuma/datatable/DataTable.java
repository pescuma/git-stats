package org.pescuma.datatable;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Predicate;

public interface DataTable {
	
	boolean isEmpty();
	
	void add(double value, String... info);
	
	void inc(double value, String... info);
	
	void add(DataTable other);
	
	void inc(DataTable other);
	
	double get(String... info);
	
	Collection<Line> getLines();
	
	Collection<String> getDistinct(int column);
	
	Collection<String[]> getDistinct(int... columns);
	
	Map<String, Value> sumDistinct(int columns);
	
	Map<String[], Value> sumDistinct(int... columns);
	
	DataTable filter(String... info);
	
	DataTable filter(int column, String value);
	
	DataTable filter(Predicate<Line> predicate);
	
	DataTable filter(int column, Predicate<String> predicate);
	
	double sum();
	
	int size();
	
	Collection<String> getColumn(int column);
	
	/** @param column null or empty to get all */
	Collection<String[]> getColumns(int... columns);
	
	public interface Line {
		double getValue();
		
		String getColumn(int column);
		
		/** @param column null or empty to get all */
		String[] getColumns(int... columns);
	}
	
	public static class Value {
		public double value;
		
		@Override
		public String toString() {
			return Double.toString(value);
		}
	}
}
