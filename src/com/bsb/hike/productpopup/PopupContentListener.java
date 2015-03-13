package com.bsb.hike.productpopup;

import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;

public abstract class PopupContentListener extends PlatformContentListener<PlatformContentModel>
{
	private ProductContentModel productContentModel;

	private IActivityPopup mmPop;

	public PopupContentListener(ProductContentModel productContentModel, IActivityPopup mmPop)
	{
		this.productContentModel = productContentModel;
		this.mmPop = mmPop;
	}

	public ProductContentModel getProductContentModel()
	{
		return productContentModel;
	}
	
	public IActivityPopup getListener()
	{
		return mmPop;
	}
}
