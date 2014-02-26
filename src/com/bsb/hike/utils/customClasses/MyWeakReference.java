package com.bsb.hike.utils.customClasses;

import java.lang.ref.WeakReference;

/**
 * @author : GK This class is made to override equals and hashcode functions, as reference class does not override them and hence weakreferences cannot be used in hash based
 *         datastructures.
 * */

public class MyWeakReference<T> extends WeakReference<T>
{

	public MyWeakReference(T r)
	{
		super(r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other)
	{

		boolean returnValue = super.equals(other);

		// If we're not equal, then check equality using referenced objects
		if (!returnValue && (other instanceof MyWeakReference<?>))
		{
			T value = this.get();
			if (null != value)
			{
				T otherValue = ((MyWeakReference<T>) other).get();

				// The delegate equals should handle otherValue == null
				returnValue = value.equals(otherValue);
			}
		}

		return returnValue;
	}

	@Override
	public int hashCode()
	{
		T value = this.get();
		return value != null ? value.hashCode() : super.hashCode();
	}
}
