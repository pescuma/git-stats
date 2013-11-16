package org.pescuma.gitstats;

import java.util.concurrent.atomic.AtomicInteger;

public class Author {
	final String name;
	private final AtomicInteger textLines = new AtomicInteger();
	private final AtomicInteger emptyLines = new AtomicInteger();
	
	public Author(String name) {
		this.name = name;
	}
	
	public void incTextLines() {
		textLines.incrementAndGet();
	}
	
	public void incEmptyLines() {
		emptyLines.incrementAndGet();
	}
	
	public int getTextLines() {
		return textLines.get();
	}
	
	public int getEmptyLines() {
		return emptyLines.get();
	}
	
	public int getTotalLines() {
		return textLines.get() + emptyLines.get();
	}
}