package com.bsb.hike.modules.iface;

/**
 * 
 * @author Gautam
 * This interface should be implemented by all those cache or modules, who wish to 
 * load and clean the cache according to different scenarios
 */
public interface ITransientCache
{
	public void load();
	public void unload();
}
