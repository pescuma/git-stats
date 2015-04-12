package org.pescuma.gitstats;

import static java.lang.Math.*;

import java.util.List;

// Based on http://rosettacode.org/wiki/Sparkline_in_unicode#Java
public class Sparkline {
	private static String bars = "▁▂▃▄▅▆▇█";
	
	public static String getSparkline(List<Double> data) {
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (Double d : data) {
			if (d < min)
				min = d;
			if (d > max)
				max = d;
		}
		
		double range = max - min;
		int num = bars.length() - 1;
		
		StringBuilder result = new StringBuilder();
		for (Double d : data)
			result.append(bars.charAt((int) round((d - min) / range * num)));
		return result.toString();
	}
}
