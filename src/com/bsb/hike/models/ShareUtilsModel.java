package com.bsb.hike.models;


public class ShareUtilsModel
{
	String stickerHeading, stickerDescription, stickerCaption;
	String imageHeading, imageDescription, imageCaption;
	String textHeading, textCaption;
	boolean showShareFunctionality,shareFunctionalityPalette;
	
	public ShareUtilsModel(String stickerHeading, String stickerDescription, String stickerCaption, String imageHeading, String imageDescription, String imageCaption,
			String textHeading, String textCaption)
	{

		this.stickerHeading = stickerHeading;
		this.stickerDescription = stickerDescription;
		this.stickerCaption = stickerCaption;
		this.imageHeading = imageHeading;
		this.imageDescription = imageDescription;
		this.imageCaption = imageCaption;
		this.textHeading = textHeading;
		this.textCaption = textCaption;
	}

	public ShareUtilsModel(boolean showShareFunctionality,boolean shareFunctionalityPalette)
	{		
		this.showShareFunctionality = showShareFunctionality;
		this.shareFunctionalityPalette = shareFunctionalityPalette;

	}
				
		/**
		 * @return the stickerHeading
		 */
		public String getStickerHeading()
		{
			return stickerHeading;
		}
		/**
		 * @return the stickerDescription
		 */
		public String getStickerDescription()
		{
			return stickerDescription;
		}
		/**
		 * @return the stickerCaption
		 */
		public String getStickerCaption()
		{
			return stickerCaption;
		}
		/**
		 * @return the imageHeading
		 */
		public String getImageHeading()
		{
			return imageHeading;
		}
		/**
		 * @return the imageDescription
		 */
		public String getImageDescription()
		{
			return imageDescription;
		}
		/**
		 * @return the imageCaption
		 */
		public String getImageCaption()
		{
			return imageCaption;
		}
		/**
		 * @return the textHeading
		 */
		public String getTextHeading()
		{
			return textHeading;
		}
		/**
		 * @return the textDescription
		 */
		public String getTextCaption()
		{
			return textCaption;
		}
		/**
		 * @return the showShareFunctionality
		 */
		public boolean getShowShareFunctionality()
		{
			return showShareFunctionality;
		}
		/**
		 * @return the shareFunctionalityPalette
		 */
		public boolean getShareFunctionalityPalette()
		{
			return shareFunctionalityPalette;
		}
		
		

		
				
}



