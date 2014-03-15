package com.bsb.hike.thor.crypt;

import java.util.Comparator;
import java.util.Map;

class sorter implements Comparator<String>
{
	Map map;

	public sorter(Map map)
	{
		this.map = map;
	}

	public int compare(String keyA, String keyB)
	{
		Comparable valueA = (Comparable) map.get(keyA);
		Comparable valueB = (Comparable) map.get(keyB);
		// Integer valueA = (Integer) map.get(keyA);
		// Integer valueB = (Integer) map.get(keyB);
		// System.out.println(valueA + " - " + valueB);
		if (valueA.equals(valueB))
			return 1;
		return valueB.compareTo(valueA);
		// return ((valueB <= valueA) ? -1 : 1);
	}
}