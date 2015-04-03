package org.pescuma.gitstats;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ColumnsOutput {
	
	public static enum Align {
		Left,
		Right
	}
	
	private List<Line> lines = new ArrayList<Line>();
	private Line currentLine;
	
	public ColumnsOutput() {
		newLine();
	}
	
	public ColumnsOutput newLine() {
		currentLine = new Line();
		lines.add(currentLine);
		return this;
	}
	
	public ColumnsOutput appendColumn(int num) {
		return appendColumn(Align.Right, "%d", num);
	}
	
	public ColumnsOutput appendColumn(String text, Object... args) {
		return appendColumn(Align.Left, text, args);
	}
	
	public ColumnsOutput appendColumn(Align align, String text, Object... args) {
		if (args.length < 1)
			currentLine.append(text, align);
		else
			currentLine.append(String.format(text, args), align);
		
		return this;
	}
	
	private static class Line {
		private List<Column> columns = new ArrayList<Column>();
		
		public void append(String text, Align align) {
			if (text == null)
				text = "";
			columns.add(new Column(text, align));
		}
		
		public String get(int index, int width) {
			if (columns.size() <= index)
				return StringUtils.repeat(' ', width);
			else
				return columns.get(index).get(width);
		}
		
		public int getLength(int index) {
			if (columns.size() <= index)
				return 0;
			else
				return columns.get(index).getLength();
		}
		
		public int getNumOfColumns() {
			return columns.size();
		}
		
		public boolean isEmpty() {
			return columns.isEmpty();
		}
	}
	
	private static class Column {
		private String text;
		private Align align;
		
		public Column(String text, Align align) {
			super();
			this.text = text;
			this.align = align;
		}
		
		public String get(int width) {
			if (align == Align.Left)
				return StringUtils.rightPad(text, width);
			else
				return StringUtils.leftPad(text, width);
		}
		
		public int getLength() {
			return text.length();
		}
		
	}
	
	public void print(PrintStream out) {
		int cols = 1;
		for (Line line : lines)
			cols = Math.max(cols, line.getNumOfColumns());
		
		int[] width = new int[cols];
		for (Line line : lines)
			for (int i = 0; i < cols; i++)
				width[i] = Math.max(width[i], line.getLength(i));
		
		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			
			if (i == lines.size() - 1 && line.isEmpty())
				break;
			
			for (int j = 0; j < line.getNumOfColumns(); j++)
				out.print(line.get(j, width[j]));
			out.println();
		}
	}
}
