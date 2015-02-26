package com.bsb.hike.productpopup;


/**
 * 
 * @author himanshu
 * 
 * This class has the information that the HikeDialogFragment may need to function properly.
 *
 */
public class DialogPojo
{

	boolean isFullScreen;

	int height;

	String html;
	
	//Any Addition data that the user want to persist.(optional hence not included in the constructor.)
	Object data;

	
	/**
	 * @param isFullScreen
	 * @param heigth
	 * @param html
	 */
	public DialogPojo(boolean isFullScreen, int heigth, String html)
	{
		this.isFullScreen = isFullScreen;
		this.height = heigth;
		this.html = html;
	}

	
	/**
	 * @return the isFullScreen
	 */
	public boolean isFullScreen()
	{
		return isFullScreen;
	}

	/**
	 * @return the heigth
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * @return the html
	 */
	public String getFormedData()
	{
		return html;
	}
	
	/**
	 * @return the data
	 */
	public Object getData()
	{
		return data;
	}


	/**
	 * @param data the data to set
	 */
	public void setData(Object data)
	{
		this.data = data;
	}


	

	
}
