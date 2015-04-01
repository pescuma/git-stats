package org.pescuma.gitstats;

import java.util.HashMap;
import java.util.Map;

// Based on https://code.google.com/p/gitinspector/source/browse/gitinspector/comment.py
public class SimpleFileParser {
	
	public enum LineType {
		Empty,
		Comment,
		Code
	}
	
	private static Map<String, Language> languages = new HashMap<String, Language>();
	static {
		Language c = new Language("/*", "*/", "//");
		languages.put("C", c);
		languages.put("C++", c);
		languages.put("C/C++ Header", c);
		languages.put("Java", c);
		languages.put("PHP", c);
		languages.put("OpenGL Shading Language", c);
		languages.put("Javascript", c);
		languages.put("Scala", c);
		
		Language html = new Language("<!--", "-->");
		languages.put("HTML", html);
		languages.put("JSP", html);
		languages.put("XML", html);
		languages.put("Ant", html);
		languages.put("Maven", html);
		
		Language sql = new Language("/*", "*/", "--");
		languages.put("SQL", sql);
		languages.put("SQL Stored Procedure", sql);
		languages.put("SQL Data", sql);
		
		languages.put("Haskell", new Language("{-", "-}", "--"));
		languages.put("Perl", new Language("#"));
		languages.put("Python", new Language("\"\"\"", "\"\"\"", "#"));
		languages.put("Ruby", new Language("=begin", "=end", "#"));
		languages.put("Ruby", new Language("=begin", "=end", "#"));
		languages.put("Tex", new Language("\\begin{comment}", "\\end{comment}", "%", true));
		languages.put("Ada", new Language("--"));
		languages.put("ML", new Language("(*", "*)"));
		languages.put("GNU Gettext", new Language("#"));
		
	}
	
	private Language language;
	private boolean insideComment;
	
	public SimpleFileParser(String filename) {
		String lang = FilenameToLanguage.detectLanguage(filename);
		if (lang != null)
			language = languages.get(lang);
		
		if (language == null)
			language = new Language(null);
	}
	
	public LineType feedNextLine(String line) {
		line = line.trim();
		
		if (line.isEmpty())
			return LineType.Empty;
		
		if (language.commentLine != null && line.startsWith(language.commentLine))
			return LineType.Comment;
		
		if (language.commentBegin != null) {
			if (language.markersMustBeAtBegining)
				return processBeginEndOnlyAtBegining(line);
			else
				return processBeginEnd(line);
		}
		
		return LineType.Code;
	}
	
	private LineType processBeginEndOnlyAtBegining(String line) {
		if (insideComment) {
			if (line.startsWith(language.commentEnd)) {
				insideComment = false;
				
				return line.substring(language.commentEnd.length()).isEmpty() ? LineType.Comment
						: LineType.Code;
			} else {
				return LineType.Comment;
			}
			
		} else {
			if (line.startsWith(language.commentBegin)) {
				insideComment = true;
				return LineType.Comment;
			} else {
				return LineType.Code;
			}
		}
	}
	
	private LineType processBeginEnd(String line) {
		boolean hasCode = false;
		boolean hasComment = false;
		
		int pos = 0;
		int length = line.length();
		do {
			int posStart = line.indexOf(language.commentBegin, pos);
			int posEnd = line.indexOf(language.commentEnd, pos);
			
			if (insideComment) {
				hasComment = true;
				
				if (posEnd < 0) {
					pos = length;
				} else {
					pos = posEnd + language.commentEnd.length();
					insideComment = false;
				}
				
			} else { // if (!insideComment) {
				if (posStart < 0) {
					hasCode = hasCode || !line.substring(pos).trim().isEmpty();
					
					pos = length;
					
				} else {
					hasCode = hasCode || !line.substring(pos, posStart).trim().isEmpty();
					hasComment = true;
					
					pos = posStart + language.commentBegin.length();
					insideComment = true;
				}
			}
			
		} while (pos < length);
		
		if (hasCode)
			return LineType.Code;
		
		if (hasComment)
			return LineType.Comment;
		
		return LineType.Code;
	}
	
	private static class Language {
		public final String commentBegin;
		public final String commentEnd;
		public final String commentLine;
		public final boolean markersMustBeAtBegining;
		
		public Language(String commentBegin, String commentEnd, String commentLine,
				boolean markersMustBeAtBegining) {
			this.commentBegin = commentBegin;
			this.commentEnd = commentEnd;
			this.commentLine = commentLine;
			this.markersMustBeAtBegining = markersMustBeAtBegining;
		}
		
		public Language(String commentBegin, String commentEnd, String commentLine) {
			this(commentBegin, commentEnd, commentLine, false);
		}
		
		public Language(String commentLine) {
			this(null, null, commentLine, false);
		}
		
		public Language(String commentBegin, String commentEnd) {
			this(commentBegin, commentEnd, null, false);
		}
	}
}
