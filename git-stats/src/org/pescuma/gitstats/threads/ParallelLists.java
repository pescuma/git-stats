package org.pescuma.gitstats.threads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ParallelLists {
	
	private final Thread[] threads;
	
	public ParallelLists(int threadCount) {
		threads = new Thread[threadCount];
	}
	
	public <T> void splitInThreads(List<T> items, final Callback<T> cb) {
		if (threads.length == 1) {
			try {
				
				cb.run(items);
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return;
		}
		
		final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>(items);
		
		final Exception[] exceptions = new Exception[threads.length];
		
		for (int i = 0; i < threads.length; i++) {
			final int index = i;
			
			threads[i] = new Thread() {
				@Override
				public void run() {
					try {
						
						Iterable<T> iterable = createIterable(queue);
						cb.run(iterable);
						
					} catch (Exception e) {
						e.printStackTrace();
						exceptions[index] = e;
					}
				}
			};
		}
		
		for (int i = 0; i < threads.length; i++)
			threads[i].start();
		
		for (int i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO
				e.printStackTrace();
			}
		}
		
		List<Exception> exs = new ArrayList<Exception>();
		for (int i = 0; i < threads.length; i++)
			if (exceptions[i] != null)
				exs.add(exceptions[i]);
		
		if (exs.size() > 0)
			// TODO
			throw new RuntimeException(exs.get(0));
	}
	
	private <T> Iterable<T> createIterable(final ConcurrentLinkedQueue<T> queue) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					T next;
					
					@Override
					public boolean hasNext() {
						if (next == null)
							next = queue.poll();
						
						return next != null;
					}
					
					@Override
					public T next() {
						try {
							return next;
						} finally {
							next = null;
						}
					}
					
					@Override
					public void remove() {
					}
				};
			}
		};
	}
	
	public interface Callback<T> {
		void run(Iterable<T> t) throws Exception;
	}
}
