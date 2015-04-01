package org.pescuma.gitstats;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SimpleFileParserTest {
	
	private SimpleFileParser java;
	private SimpleFileParser tex;
	
	@Before
	public void setup() {
		java = new SimpleFileParser("a.java");
		tex = new SimpleFileParser("a.tex");
	}
	
	@Test
	public void testEmpty() {
		assertEquals(SimpleFileParser.LineType.Empty, java.feedNextLine(""));
	}
	
	@Test
	public void testLineCommentAtStart() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("//a"));
	}
	
	@Test
	public void testLineCommentAtMiddle() {
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("a //a"));
	}
	
	@Test
	public void testCode() {
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("a"));
	}
	
	@Test
	public void testMultipleComment_Start() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*a"));
	}
	
	@Test
	public void testMultipleComment_StartAtMiddle() {
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("a/*a"));
	}
	
	@Test
	public void testMultipleComment_SecondLine() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("a"));
	}
	
	@Test
	public void testMultipleComment_End() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("a"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("*/"));
	}
	
	@Test
	public void testMultipleComment_AfterEnd() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("a"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("*/"));
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("a"));
	}
	
	@Test
	public void testMultipleComment_EndAtMiddle() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("a"));
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("*/b"));
	}
	
	@Test
	public void testMultipleComment_SameLineNothingElse() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/**//**//**/"));
	}
	
	@Test
	public void testMultipleComment_SameLineNoCode() {
		assertEquals(SimpleFileParser.LineType.Comment,
				java.feedNextLine("  /* x*/\t/*b*/ /* a */"));
	}
	
	@Test
	public void testMultipleComment_SameLineWithCodeInBetween() {
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("/**/a/**/"));
	}
	
	@Test
	public void testMultipleComment_WithCodeInBetween() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*"));
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("*/a/*"));
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("*/"));
	}
	
	@Test
	public void testMultipleComment_StartInsideComment() {
		assertEquals(SimpleFileParser.LineType.Comment, java.feedNextLine("/*a/*b*/"));
	}
	
	@Test
	public void testMultipleComment_EndBeforeStart() {
		assertEquals(SimpleFileParser.LineType.Code, java.feedNextLine("*//*"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_BeforeStart() {
		assertEquals(SimpleFileParser.LineType.Code, tex.feedNextLine("a"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_Start() {
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\begin{comment}"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_StartAtMiddle() {
		assertEquals(SimpleFileParser.LineType.Code, tex.feedNextLine("a\\begin{comment}"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_SecondLine() {
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\begin{comment}"));
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("a"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_End() {
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\begin{comment}"));
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("a"));
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\end{comment}"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_AfterEnd() {
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\begin{comment}"));
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("a"));
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\end{comment}"));
		assertEquals(SimpleFileParser.LineType.Code, tex.feedNextLine("a"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_EndAtMiddle() {
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("\\begin{comment}"));
		assertEquals(SimpleFileParser.LineType.Code, tex.feedNextLine("\\end{comment}a"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_SingleLine() {
		assertEquals(SimpleFileParser.LineType.Comment, tex.feedNextLine("%a"));
	}
	
	@Test
	public void testMarkersMustBeAtBegining_SingleLineAtMiddle() {
		assertEquals(SimpleFileParser.LineType.Code, tex.feedNextLine("a%a"));
	}
	
}
