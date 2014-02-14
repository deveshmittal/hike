package com.bsb.hike.utils.customClasses;

import java.lang.ref.SoftReference;

/**
 * @author : GK
 *  This class is made to override equals and hashcode functions, as reference class does not override them
 *  and hence softreferences cannot be used in hash based datastructures.
 * */
 
public class MySoftReference<T> extends SoftReference<T>
{

	public MySoftReference(T r)
	{
		super(r);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other)
	{

		boolean returnValue = super.equals(other);

		// If we're not equal, then check equality using referenced objects
		if (!returnValue && (other instanceof MySoftReference<?>))
		{
			T value = this.get();
			if (null != value)
			{
				T otherValue = ((MySoftReference<T>) other).get();

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
