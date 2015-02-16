package com.bsb.hike.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.photos.FilterTools.FilterType;
import com.bsb.hike.photos.HikeEffectsFactory.OnPreviewReadyListener;

abstract class EffectItem extends LinearLayout 
{

	private int ForegroundColor;
	private int BackgroundColor;
	private TextView label;
	private ImageView icon;
	private Bitmap postInflate;


	public EffectItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub


	}
	public EffectItem(Context context) {
		super(context);
	}

	public String getText()
	{
		return (String) this.label.getText();
	}

	public void setText(String text)
	{
		this.label.setGravity(Gravity.CENTER);
		this.label.setText(text);
		this.label.invalidate();
		this.invalidate();
	}

	public int getBackgroundColor()
	{
		return this.BackgroundColor;
	}

	public int getForegroundColor()
	{
		return this.ForegroundColor;
	}

	public void setForegroundColor(int Color)
	{
		this.label.setTextColor(getResources().getColor(Color));
		this.label.invalidate();
		this.invalidate();


	}

	public void setBackgroundColor(int Color)
	{
		this.setBackgroundColor(getResources().getColor(Color));
		this.invalidate();

	}

	public void setImage(Drawable drawable)
	{
		this.icon.setImageDrawable(drawable);
		this.icon.invalidate();
		this.invalidate();
	}

	public void setImage(Bitmap bitmap)
	{
		if(this.icon!=null)
		{
			this.icon.setImageBitmap(bitmap);
			this.icon.invalidate();
		}
		else
			postInflate=bitmap;
		this.invalidate();
	}

	public Bitmap getIcon(){
		return ((BitmapDrawable)this.icon.getDrawable()).getBitmap();

	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		try{
			label= (TextView) findViewById(R.id.previewText);
		}
		catch(Exception e)
		{

		}
		icon=(ImageView) findViewById(R.id.previewIcon);
		if(postInflate!=null)
			setImage(postInflate);
	}


}



//preview type to be changed to bitmap later

class FilterEffectItem extends EffectItem implements OnPreviewReadyListener 
{
	private FilterType filter;

	public FilterEffectItem(Context context, AttributeSet attrs) {
		super(context,attrs);

	}

	public void init(Bitmap preview,String Title)
	{

		preview=Bitmap.createScaledBitmap(preview, PhotoEditerTools.dpToPx(this.getContext(), 80) , PhotoEditerTools.dpToPx(this.getContext(),80), false);
		this.setImage(preview);
		this.setText(Title);

	}

	public void setFilter(Context context,FilterType type) {
		this.filter=type;
		HikeEffectsFactory.LoadPreviewThumbnail(this.getIcon(), type, this); 
	}

	public FilterType getFilter() {
		return filter;
	}

	@Override
	public void onPreviewReady(Bitmap preview) {
		// TODO Auto-generated method stub
		setImage(preview);
	}



}

/*class BorderEffectItem extends EffectItem 
{
	private int borderId;
	public BorderEffectItem(Context context,Drawable preview,String title) {
		super(context);
		this.setImage(preview);
		this.setText(title);

		// TODO Auto-generated constructor stub
	}

	public void setBorderId(int borderId) {
		this.borderId = borderId;
	}

	public Drawable getBorder() {
		return getResources().getDrawable(borderId);

	}


}
 */
class DoodleEffectItem extends EffectItem
{
	private int BrushWidth;
	private int BrushColor;
	private int RingColor;
	
	public DoodleEffectItem(Context context, AttributeSet attrs) {
		super(context,attrs);
		// TODO Auto-generated constructor stub
		BrushWidth=PhotoEditerTools.dpToPx(context, 30) ;
		BrushColor=0xFF000000;
		RingColor=0xFFFFFFFF;
		setImage(getCircleIcon());
	}

	public void Refresh()
	{


		setImage(getCircleIcon()); 
		invalidate();
	}

	public int getBrushColor()
	{
		return BrushColor;
	}

	public void setBrushColor(int Color) {
		BrushColor = Color;
	}

	public int getRingColor()
	{
		return RingColor;
	}

	public void setRingColor(int Color) {
		RingColor = Color;
	}


	private Bitmap getCircleIcon()
	{
		int diameter=BrushWidth+PhotoEditerTools.dpToPx(this.getContext(), 4);
		Bitmap bitmap=Bitmap.createBitmap(diameter, diameter, Config.ARGB_8888 );
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(RingColor);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawCircle(diameter/2, diameter/2, (diameter/2), paint);
		paint.setColor(BrushColor);
		canvas.drawCircle(diameter/2, diameter/2, (BrushWidth/2) , paint);
		return bitmap;
	}

	public void setBrushWidth(int brushWidth) {
		BrushWidth = brushWidth;
	}

	public int getBrushWidth() {
		return BrushWidth;
	}


}


