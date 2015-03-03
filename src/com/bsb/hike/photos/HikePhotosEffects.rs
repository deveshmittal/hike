

#pragma version(1)
#pragma rs java_package_name(com.bsb.hike.photos)
#pragma rs_fp_relaxed

#define round(x) ((x)>=0?(int)((x)+0.5):(int)((x)-0.5))
#define ChannelBlend_Normal(A,B)     ((A))
#define ChannelBlend_Lighten(A,B)    (((B > A) ? B:A))
#define ChannelBlend_Darken(A,B)     (((B > A) ? A:B))
#define ChannelBlend_Multiply(A,B)   (((A * B) / 255))
#define ChannelBlend_Average(A,B)    (((A + B) / 2))
#define ChannelBlend_Add(A,B)        ((min(255, (A + B))))
#define ChannelBlend_Subtract(A,B)   (((A + B < 255) ? 0:(A + B - 255)))
#define ChannelBlend_Difference(A,B) ((abs(A - B)))
#define ChannelBlend_Negation(A,B)   ((255 - abs(255 - A - B)))
#define ChannelBlend_Screen(A,B)     ((255 - (((255 - A) * (255 - B)) >> 8)))
#define ChannelBlend_Exclusion(A,B)  ((A + B - 2 * A * B / 255))
#define ChannelBlend_Overlay(A,B)    (((B < 128) ? (2 * A * B / 255):(255 - 2 * (255 - A) * (255 - B) / 255)))
#define ChannelBlend_HardLight(A,B)  (ChannelBlend_Overlay(B,A))
#define ChannelBlend_ColorDodge(A,B) (((B == 255) ? B:min(255, ((A << 8 ) / (255 - B)))))
#define ChannelBlend_ColorBurn(A,B)  (((B == 0) ? B:max(0, (255 - ((255 - A) << 8 ) / B))))
#define ChannelBlend_LinearDodge(A,B)(ChannelBlend_Add(A,B))
#define ChannelBlend_LinearBurn(A,B) (ChannelBlend_Subtract(A,B))
#define ChannelBlend_LinearLight(A,B)((B < 128)?ChannelBlend_LinearBurn(A,(2 * B)):ChannelBlend_LinearDodge(A,(2 * (B - 128))))
#define ChannelBlend_VividLight(A,B) ((B < 128)?ChannelBlend_ColorBurn(A,(2 * B)):ChannelBlend_ColorDodge(A,(2 * (B - 128))))
#define ChannelBlend_PinLight(A,B)   ((B < 128)?ChannelBlend_Darken(A,(2 * B)):ChannelBlend_Lighten(A,(2 * (B - 128))))
#define ChannelBlend_HardMix(A,B)    (((ChannelBlend_VividLight(A,B) < 128) ? 0:255))
#define ChannelBlend_Reflect(A,B)    (((B == 255) ? B:min(255, (A * A / (255 - B)))))
#define ChannelBlend_Glow(A,B)       (ChannelBlend_Reflect(B,A))
#define ChannelBlend_Phoenix(A,B)    ((min(A,B) - max(A,B) + 255))
#define ChannelBlend_Alpha(A,B,O)    ((O * A + (1 - O) * B))
#define ChannelBlend_AlphaF(A,B,F,O) (ChannelBlend_Alpha(F(A,B),A,O))
#define ChannelBlend_SoftLight(A,B)  (((B < 128)?(2*((A>>1)+64))*((float)B/255):(255-(2*(255-((A>>1)+64))*(float)(255-B)/255))))

float static d(float x) 
{
	if (x <= 0.25) {
		return x * (4 + x*(16*x - 12));
	}
	else {

		return sqrt(x);
	}
}

int static SoftLight(float fg, float bg) 
{
	fg = fg/255.0;
	bg = bg/255.0;
	float res=0;
	if (fg <= 0.5) {
		res = bg - (1 - 2*fg) * bg * (1 - bg);
	} else {
		res = bg + (2 * fg - 1) * (d(bg) - bg);
	}
	return round(res*255.0);

}

int rSpline[256];
int gSpline[256];
int bSpline[256];
int compositeSpline[256];

int r[3],g[3],b[3];

rs_allocation input1;
rs_allocation input2;


uchar4 __attribute__((kernel)) multiply(uchar4 in,uint32_t x,uint32_t y) {
	in.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(r[0],in.r),in.r,0.50);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(g[0],in.g),in.g,0.50);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(b[0],in.b),in.b,0.50);

	return in;
}

uchar4 __attribute__((kernel)) filter1(uchar4 in,uint32_t x,uint32_t y) {


	in.r =  ChannelBlend_Alpha(ChannelBlend_Exclusion(r[0],in.r),in.r,0.30);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Exclusion(g[0],in.g),in.g,0.30);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Exclusion(b[0],in.b),in.b,0.30);

	in.r =  ChannelBlend_Alpha(SoftLight(r[1],in.r),in.r,0.75);

	in.g =  ChannelBlend_Alpha(SoftLight(g[1],in.g),in.g,0.75);

	in.b =  ChannelBlend_Alpha(SoftLight(b[1],in.b),in.b,0.75);


	return in;
}

uchar4 __attribute__((kernel)) filter_1977_or_xpro(uchar4 in,uint32_t x,uint32_t y) {

	in.r=rSpline[in.r];

	in.g=gSpline[in.g];

	in.b=bSpline[in.b];

	return in;
}

uchar4 __attribute__((kernel)) filter_classic(uchar4 in,uint32_t x,uint32_t y) {

	in.r=rSpline[in.r];

	in.g=gSpline[in.g];

	in.b=bSpline[in.b];


	in.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(r[0],in.r),in.r,0.50);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(g[0],in.g),in.g,0.50);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(b[0],in.b),in.b,0.50);

	in.r =  ChannelBlend_Exclusion(r[1],in.r);

	in.g =  ChannelBlend_Exclusion(g[1],in.g);

	in.b =  ChannelBlend_Exclusion(b[1],in.b);

	return in;
}

uchar4 __attribute__((kernel)) filter_kelvin(uchar4 in,uint32_t x,uint32_t y) {

	in.r=rSpline[in.r];

	in.g=gSpline[in.g];

	in.b=bSpline[in.b];


	in.r =  ChannelBlend_Alpha(ChannelBlend_Overlay(r[0],in.r),in.r,0.30);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Overlay(g[0],in.g),in.g,0.30);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Overlay(b[0],in.b),in.b,0.30);


	return in;
}

uchar4 __attribute__((kernel)) filter_retro(uchar4 in,uint32_t x,uint32_t y) {

	in.r=compositeSpline[in.r];

	in.g=compositeSpline[in.g];

	in.b=compositeSpline[in.b];

	in.b=bSpline[in.b];


	in.r =  ChannelBlend_Alpha(ChannelBlend_Multiply(r[0],in.r),in.r,0.60);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Multiply(g[0],in.g),in.g,0.60);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Multiply(b[0],in.b),in.b,0.60);

	return in;
}

uchar4 __attribute__((kernel)) filter_brannan(uchar4 in,uint32_t x,uint32_t y) 
{
	in.r =  ChannelBlend_Alpha(ChannelBlend_Overlay(r[0],in.r),in.r,0.70);

	in.g =  ChannelBlend_Alpha(ChannelBlend_Overlay(g[0],in.g),in.g,0.70);

	in.b =  ChannelBlend_Alpha(ChannelBlend_Overlay(b[0],in.b),in.b,0.70);
	
	in.b = bSpline[in.b];
	
	return in;
}

uchar4 __attribute__((kernel)) filter_earlyBird(uchar4 in,uint32_t x,uint32_t y) 
{
	in.r = ChannelBlend_Multiply(r[0],in.r);
	
	in.g = ChannelBlend_Multiply(g[0],in.g);
	
	in.b = ChannelBlend_Multiply(b[0],in.b);
	
	return in;
}

uchar4 __attribute__((kernel)) filter_inkwell_or_lomofi(uchar4 in,uint32_t x,uint32_t y) {

	in.r=compositeSpline[in.r];

	in.g=compositeSpline[in.g];

	in.b=compositeSpline[in.b];

	return in;
}
