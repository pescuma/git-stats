package org.pescuma.gitstats;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Authors {
	private final ConcurrentMap<String, Author> authors = new ConcurrentHashMap<String, Author>();
	
	public Author get(String authorName) {
		Author author = authors.get(authorName);
		if (author == null) {
			author = new Author(authorName);
			Author other = authors.putIfAbsent(authorName, author);
			if (other != null)
				author = other;
		}
		return author;
	}
	
	public Collection<Author> values() {
		return authors.values();
	}
}
