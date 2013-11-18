package org.pescuma.gitstats;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jgit.revwalk.RevCommit;

public class DateInfo {
	
	private final String name;
	private final AtomicInteger commits = new AtomicInteger();
	private final AtomicInteger emptyLines = new AtomicInteger();
	private final AtomicInteger textLines = new AtomicInteger();
	
	public DateInfo(String name) {
		this.name = name;
	}
	
	public void addCommit(RevCommit commit) {
		commits.incrementAndGet();
	}
	
	public void addLine(RevCommit commit, boolean emptyLine) {
		if (emptyLine)
			emptyLines.incrementAndGet();
		else
			textLines.incrementAndGet();
	}
	
	public String getName() {
		return name;
	}
	
	public int getCommitCount() {
		return commits.get();
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
