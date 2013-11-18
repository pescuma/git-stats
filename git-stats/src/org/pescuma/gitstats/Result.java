package org.pescuma.gitstats;

public class Result {
	
	public final CommitsStats commits = new CommitsStats();
	public final Authors authors = new Authors();
	public final Author unblamable = new Author("Unblamable");
}
