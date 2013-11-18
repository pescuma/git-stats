package org.pescuma.gitstats;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jgit.revwalk.RevCommit;

public class Author {
	
	private final String name;
	private final CommitsStats commits = new CommitsStats();
	private final AtomicInteger textLines = new AtomicInteger();
	private final AtomicInteger emptyLines = new AtomicInteger();
	
	public Author(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public CommitsStats getCommits() {
		return commits;
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
	
	public void add(RevCommit commit, boolean emptyLine) throws IOException {
		if (commit != null)
			commits.add(commit, emptyLine);
		
		if (emptyLine)
			emptyLines.incrementAndGet();
		else
			textLines.incrementAndGet();
	}
}
