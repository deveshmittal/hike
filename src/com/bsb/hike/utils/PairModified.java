package com.bsb.hike.utils;

/*
 *   This class is replacement for java pair class {@link Pair}. In pair class the first and second fields are both final so cannot be modified.
 *   This class overcomes this limitation and can be used in cases where we need to modify pair fields
 */
public class PairModified<L, R>
{
	private L l;

	private R r;

	public PairModified(L l, R r)
	{
		this.l = l;
		this.r = r;
	}

	public L getFirst()
	{
		return l;
	}

	public R getSecond()
	{
		return r;
	}

	public void setFirst(L l)
	{
		this.l = l;
	}

	public void setSecond(R r)
	{
		this.r = r;
	}
}
