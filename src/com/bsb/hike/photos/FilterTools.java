package com.bsb.hike.photos;

import java.util.ArrayList;
import java.util.List;

public class FilterTools
{

	public enum FilterType
	{
		BRIGHTNESS, CONTRAST, SATURATION, HUE, SEPIA, GRAYSCALE, POLAROID, FADED, BGR, INVERSION, X_PRO_2, WILLOW, WALDEN, VALENCIA, TOASTER, SUTRO, SIERRA, RISE, NASHVILLE, MAYFAIR, LO_FI, KELVIN, INKWELL, HUDSON, HEFE, EARLYBIRD, BRANNAN, AMARO, E1977
	}

	public static class FilterList
	{
		public List<String> names = new ArrayList<String>();

		public List<FilterType> filters = new ArrayList<FilterType>();

		private static FilterList effectfilters, qualityfilters;

		public void addFilter(final String name, final FilterType filter)
		{
			names.add(name);
			filters.add(filter);
		}

		// X_PRO_2,WILLOW,WALDEN,VALENCIA,TOASTER,SUTRO,SIERRA,RISE,NASHVILLE,MAYFAIR,LO_FI,KELVIN,INKWELL,HUDSON,HEFE,EARLYBIRD,BRANNAN,AMARO,E1977
		// }

		

		public static FilterList getHikeEffects()
		{
			if (effectfilters == null)
			{
				effectfilters = new FilterList();
				effectfilters.addFilter("Original", FilterType.AMARO);
				effectfilters.addFilter("Faded", FilterType.FADED);
				effectfilters.addFilter("Polaroid", FilterType.POLAROID);
				effectfilters.addFilter("Sepia", FilterType.SEPIA);
				effectfilters.addFilter("Grayscale", FilterType.GRAYSCALE);
				effectfilters.addFilter("X PRO 2", FilterType.X_PRO_2);
				effectfilters.addFilter("Willow", FilterType.WILLOW);
				effectfilters.addFilter("Walden", FilterType.WALDEN);
				effectfilters.addFilter("Toaster", FilterType.TOASTER);
				effectfilters.addFilter("Sutro", FilterType.SUTRO);
				effectfilters.addFilter("Sierra", FilterType.SIERRA);
				effectfilters.addFilter("Rise", FilterType.RISE);
//				effectfilters.addFilter("NashVille", FilterType.NASHVILLE);
				effectfilters.addFilter("Lo Fi", FilterType.LO_FI);
				effectfilters.addFilter("Kelvin", FilterType.KELVIN);
				effectfilters.addFilter("Inkwell", FilterType.INKWELL);
//				effectfilters.addFilter("Hudson", FilterType.HUDSON);
//				effectfilters.addFilter("Hefe", FilterType.HEFE);
//				effectfilters.addFilter("EarlyBird", FilterType.EARLYBIRD);
//				effectfilters.addFilter("Brannan", FilterType.BRANNAN);
//				effectfilters.addFilter("Amaro", FilterType.AMARO);
//				effectfilters.addFilter("E1977", FilterType.E1977);

				/*
				 * effectfilters.addFilter("Sobel Edge Detection", FilterType.SOBEL_EDGE_DETECTION); effectfilters.addFilter("Emboss", FilterType.EMBOSS);
				 * effectfilters.addFilter("Posterize", FilterType.POSTERIZE); effectfilters.addFilter("Grouped effectfilters", FilterType.FILTER_GROUP);
				 * effectfilters.addFilter("Monochrome", FilterType.MONOCHROME); effectfilters.addFilter("RGB", FilterType.RGB); effectfilters.addFilter("Vignette",
				 * FilterType.VIGNETTE); effectfilters.addFilter("Lookup (Amatorka)", FilterType.LOOKUP_AMATORKA); effectfilters.addFilter("CGA Color Space",
				 * FilterType.CGA_COLORSPACE); effectfilters.addFilter("Sketch", FilterType.SKETCH); effectfilters.addFilter("Toon", FilterType.TOON);
				 * effectfilters.addFilter("Smooth Toon", FilterType.SMOOTH_TOON); effectfilters.addFilter("Bulge Distortion", FilterType.BULGE_DISTORTION);
				 * effectfilters.addFilter("Glass Sphere", FilterType.GLASS_SPHERE); effectfilters.addFilter("Laplacian", FilterType.LAPLACIAN); effectfilters.addFilter("Swirl",
				 * FilterType.SWIRL); effectfilters.addFilter("Color Balance", FilterType.COLOR_BALANCE);
				 */
			}
			return effectfilters;

		}

		public static FilterList getQualityFilters()
		{
			if (qualityfilters == null)
			{
				qualityfilters = new FilterList();

				/*
				 * qualityfilters.addFilter("Contrast", FilterType.CONTRAST); qualityfilters.addFilter("Gamma", FilterType.GAMMA); qualityfilters.addFilter("Brightness",
				 * FilterType.BRIGHTNESS); qualityfilters.addFilter("Sharpness", FilterType.SHARPEN); qualityfilters.addFilter("3x3 Convolution",
				 * FilterType.THREE_X_THREE_CONVOLUTION); qualityfilters.addFilter("Saturation", FilterType.SATURATION); qualityfilters.addFilter("Exposure", FilterType.EXPOSURE);
				 * qualityfilters.addFilter("Highlight Shadow", FilterType.HIGHLIGHT_SHADOW); qualityfilters.addFilter("Opacity", FilterType.OPACITY);
				 * qualityfilters.addFilter("RGB", FilterType.RGB); qualityfilters.addFilter("White Balance", FilterType.WHITE_BALANCE); qualityfilters.addFilter("ToneCurve",
				 * FilterType.TONE_CURVE); qualityfilters.addFilter("Gaussian Blur", FilterType.GAUSSIAN_BLUR); qualityfilters.addFilter("Crosshatch", FilterType.CROSSHATCH);
				 * qualityfilters.addFilter("Box Blur", FilterType.BOX_BLUR); qualityfilters.addFilter("Dilation", FilterType.DILATION); qualityfilters.addFilter("Kuwahara",
				 * FilterType.KUWAHARA); qualityfilters.addFilter("RGB Dilation", FilterType.RGB_DILATION); qualityfilters.addFilter("Haze", FilterType.HAZE);
				 */
			}
			return qualityfilters;

		}
	}

}