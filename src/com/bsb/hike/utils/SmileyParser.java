package com.bsb.hike.utils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.widget.EditText;

import com.bsb.hike.R;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons to graphical ones.
 */
public class SmileyParser
{
	private static int MAX_EMOTICONS = 10;

	public static final int[] SIZES_EMOTICON_SETS = {80, 30, 39};

	// Singleton stuff
	private static SmileyParser sInstance = null;

	public static SmileyParser getInstance()
	{
		return sInstance;
	}

	public static void init(Context context)
	{
		// GH - added a null check so instances will get reused
		if (sInstance == null)
			sInstance = new SmileyParser(context);
	}

	private final Context mContext;

	private final String[] mSmileyTexts;

	private final Pattern mPattern;

	private final HashMap<String, Integer> mSmileyToRes;

	private final HashMap<String, Integer> mSmallSmileyToRes;

	private SmileyParser(Context context)
	{
		mContext = context;
		mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
		mSmileyToRes = buildSmileyToRes();
		mSmallSmileyToRes = buildSmallSmileyToRes();
		mPattern = buildPattern();
	}

	static class Smileys
	{
		private static final int[] sIconIds = {R.drawable.emo_im_01_bigsmile,
			R.drawable.emo_im_02_happy,
			R.drawable.emo_im_03_laugh,
			R.drawable.emo_im_04_smile,
			R.drawable.emo_im_05_wink,
			R.drawable.emo_im_06_adore,
			R.drawable.emo_im_07_kiss,
			R.drawable.emo_im_08_kissed,
			R.drawable.emo_im_09_expressionless,
			R.drawable.emo_im_10_pudently,
			R.drawable.emo_im_11_satisfied,
			R.drawable.emo_im_12_giggle,
			R.drawable.emo_im_13_impish,
			R.drawable.emo_im_14_disappointment,
			R.drawable.emo_im_15_beuptonogood,
			R.drawable.emo_im_16_frustrated,
			R.drawable.emo_im_17_sad,
			R.drawable.emo_im_18_sorry,
			R.drawable.emo_im_19_cry,
			R.drawable.emo_im_20_boring,
			R.drawable.emo_im_21_hungry,
			R.drawable.emo_im_22_scared,
			R.drawable.emo_im_23_shock,
			R.drawable.emo_im_24_sweat,
			R.drawable.emo_im_25_crying,
			R.drawable.emo_im_26_lol,
			R.drawable.emo_im_27_woo,
			R.drawable.emo_im_28_surprise,
			R.drawable.emo_im_29_frown,
			R.drawable.emo_im_30_angry,
			R.drawable.emo_im_31_wornout,
			R.drawable.emo_im_32_stop,
			R.drawable.emo_im_33_furious,
			R.drawable.emo_im_34_smoking,
			R.drawable.emo_im_35_hysterical,
			R.drawable.emo_im_36_exclamation,
			R.drawable.emo_im_37_question,
			R.drawable.emo_im_38_sleep,
			R.drawable.emo_im_39_aggressive,
			R.drawable.emo_im_40_badly,
			R.drawable.emo_im_41_singing,
			R.drawable.emo_im_42_bomb,
			R.drawable.emo_im_43_beaten,
			R.drawable.emo_im_44_thumbsdown,
			R.drawable.emo_im_45_thumbsup,
			R.drawable.emo_im_46_beer,
			R.drawable.emo_im_47_call,
			R.drawable.emo_im_48_hi,
			R.drawable.emo_im_49_hug,
			R.drawable.emo_im_50_facepalm,
			R.drawable.emo_im_51_easymoney,
			R.drawable.emo_im_52_dizzy,
			R.drawable.emo_im_53_disgust,
			R.drawable.emo_im_54_cocktail,
			R.drawable.emo_im_55_coffee,
			R.drawable.emo_im_56_cold,
			R.drawable.emo_im_57_cool,
			R.drawable.emo_im_58_despair,
			R.drawable.emo_im_59_hypnotic,
			R.drawable.emo_im_60_stars,
			R.drawable.emo_im_61_idea,
			R.drawable.emo_im_62_monocle,
			R.drawable.emo_im_63_movie,
			R.drawable.emo_im_64_music,
			R.drawable.emo_im_65_nerd,
			R.drawable.emo_im_66_ninja,
			R.drawable.emo_im_67_party,
			R.drawable.emo_im_68_pirate,
			R.drawable.emo_im_69_rage,
			R.drawable.emo_im_70_rose,
			R.drawable.emo_im_71_sick,
			R.drawable.emo_im_72_snotty,
			R.drawable.emo_im_73_stressed,
			R.drawable.emo_im_74_struggle,
			R.drawable.emo_im_75_study,
			R.drawable.emo_im_76_sweetangel,
			R.drawable.emo_im_77_thinking,
			R.drawable.emo_im_78_waiting,
			R.drawable.emo_im_79_whistling,
			R.drawable.emo_im_80_yawn,
			R.drawable.emo_im_81_exciting,
			R.drawable.emo_im_82_big_smile,
			R.drawable.emo_im_83_haha,
			R.drawable.emo_im_84_victory,
			R.drawable.emo_im_85_red_heart,
			R.drawable.emo_im_86_amazing,
			R.drawable.emo_im_87_black_heart,
			R.drawable.emo_im_88_what,
			R.drawable.emo_im_89_bad_smile,
			R.drawable.emo_im_90_bad_egg,
			R.drawable.emo_im_91_grimace,
			R.drawable.emo_im_92_girl,
			R.drawable.emo_im_93_greedy,
			R.drawable.emo_im_94_anger,
			R.drawable.emo_im_95_eyes_droped,
			R.drawable.emo_im_96_happy,
			R.drawable.emo_im_97_horror,
			R.drawable.emo_im_98_money,
			R.drawable.emo_im_99_nothing,
			R.drawable.emo_im_100_nothing_to_say,
			R.drawable.emo_im_101_cry,
			R.drawable.emo_im_102_scorn,
			R.drawable.emo_im_103_secret_smile,
			R.drawable.emo_im_104_shame,
			R.drawable.emo_im_105_shocked,
			R.drawable.emo_im_106_super_man,
			R.drawable.emo_im_107_the_iron_man,
			R.drawable.emo_im_108_unhappy,
			R.drawable.emo_im_109_electric_shock,
			R.drawable.emo_im_110_beaten,
			R.drawable.emo_im_111_grin,
			R.drawable.emo_im_112_happy,
			R.drawable.emo_im_113_fake_smile,
			R.drawable.emo_im_114_in_love,
			R.drawable.emo_im_115_kiss,
			R.drawable.emo_im_116_straight_face,
			R.drawable.emo_im_117_meaw,
			R.drawable.emo_im_118_drunk,
			R.drawable.emo_im_119_x_x,
			R.drawable.emo_im_120_youre_kidding_right,
			R.drawable.emo_im_122_sweat,
			R.drawable.emo_im_123_nerd,
			R.drawable.emo_im_124_angry,
			R.drawable.emo_im_125_disappearing,
			R.drawable.emo_im_126_dizzy,
			R.drawable.emo_im_127_music,
			R.drawable.emo_im_128_evilish,
			R.drawable.emo_im_129_graffiti,
			R.drawable.emo_im_130_omg,
			R.drawable.emo_im_131_on_fire,
			R.drawable.emo_im_132_ouch,
			R.drawable.emo_im_133_angry,
			R.drawable.emo_im_134_serious_business,
			R.drawable.emo_im_135_sick,
			R.drawable.emo_im_136_slow,
			R.drawable.emo_im_137_snooty,
			R.drawable.emo_im_138_suspicious,
			R.drawable.emo_im_139_crying,
			R.drawable.emo_im_140_want,
			R.drawable.emo_im_141_we_all_gonna_die,
			R.drawable.emo_im_142_wut,
			R.drawable.emo_im_143_boo,
			R.drawable.emo_im_144_xd,
			R.drawable.emo_im_145_kaboom,
			R.drawable.emo_im_146_yarr,
			R.drawable.emo_im_147_ninja,
			R.drawable.emo_im_148_yuush,
			R.drawable.emo_im_149_brains,
			R.drawable.emo_im_150_sleeping};

		private static final int[] sIconIdsSmall = {R.drawable.emo_im_01_bigsmile_small,
			R.drawable.emo_im_02_happy_small,
			R.drawable.emo_im_03_laugh_small,
			R.drawable.emo_im_04_smile_small,
			R.drawable.emo_im_05_wink_small,
			R.drawable.emo_im_06_adore_small,
			R.drawable.emo_im_07_kiss_small,
			R.drawable.emo_im_08_kissed_small,
			R.drawable.emo_im_09_expressionless_small,
			R.drawable.emo_im_10_pudently_small,
			R.drawable.emo_im_11_satisfied_small,
			R.drawable.emo_im_12_giggle_small,
			R.drawable.emo_im_13_impish_small,
			R.drawable.emo_im_14_disappointment_small,
			R.drawable.emo_im_15_beuptonogood_small,
			R.drawable.emo_im_16_frustrated_small,
			R.drawable.emo_im_17_sad_small,
			R.drawable.emo_im_18_sorry_small,
			R.drawable.emo_im_19_cry_small,
			R.drawable.emo_im_20_boring_small,
			R.drawable.emo_im_21_hungry_small,
			R.drawable.emo_im_22_scared_small,
			R.drawable.emo_im_23_shock_small,
			R.drawable.emo_im_24_sweat_small,
			R.drawable.emo_im_25_crying_small,
			R.drawable.emo_im_26_lol_small,
			R.drawable.emo_im_27_woo_small,
			R.drawable.emo_im_28_surprise_small,
			R.drawable.emo_im_29_frown_small,
			R.drawable.emo_im_30_angry_small,
			R.drawable.emo_im_31_wornout_small,
			R.drawable.emo_im_32_stop_small,
			R.drawable.emo_im_33_furious_small,
			R.drawable.emo_im_34_smoking_small,
			R.drawable.emo_im_35_hysterical_small,
			R.drawable.emo_im_36_exclamation_small,
			R.drawable.emo_im_37_question_small,
			R.drawable.emo_im_38_sleep_small,
			R.drawable.emo_im_39_aggressive_small,
			R.drawable.emo_im_40_badly_small,
			R.drawable.emo_im_41_singing_small,
			R.drawable.emo_im_42_bomb_small,
			R.drawable.emo_im_43_beaten_small,
			R.drawable.emo_im_44_thumbsdown_small,
			R.drawable.emo_im_45_thumbsup_small,
			R.drawable.emo_im_46_beer_small,
			R.drawable.emo_im_47_call_small,
			R.drawable.emo_im_48_hi_small,
			R.drawable.emo_im_49_hug_small,
			R.drawable.emo_im_50_facepalm_small,
			R.drawable.emo_im_51_easymoney_small,
			R.drawable.emo_im_52_dizzy_small,
			R.drawable.emo_im_53_disgust_small,
			R.drawable.emo_im_54_cocktail_small,
			R.drawable.emo_im_55_coffee_small,
			R.drawable.emo_im_56_cold_small,
			R.drawable.emo_im_57_cool_small,
			R.drawable.emo_im_58_despair_small,
			R.drawable.emo_im_59_hypnotic_small,
			R.drawable.emo_im_60_stars_small,
			R.drawable.emo_im_61_idea_small,
			R.drawable.emo_im_62_monocle_small,
			R.drawable.emo_im_63_movie_small,
			R.drawable.emo_im_64_music_small,
			R.drawable.emo_im_65_nerd_small,
			R.drawable.emo_im_66_ninja_small,
			R.drawable.emo_im_67_party_small,
			R.drawable.emo_im_68_pirate_small,
			R.drawable.emo_im_69_rage_small,
			R.drawable.emo_im_70_rose_small,
			R.drawable.emo_im_71_sick_small,
			R.drawable.emo_im_72_snotty_small,
			R.drawable.emo_im_73_stressed_small,
			R.drawable.emo_im_74_struggle_small,
			R.drawable.emo_im_75_study_small,
			R.drawable.emo_im_76_sweetangel_small,
			R.drawable.emo_im_77_thinking_small,
			R.drawable.emo_im_78_waiting_small,
			R.drawable.emo_im_79_whistling_small,
			R.drawable.emo_im_80_yawn_small,
			R.drawable.emo_im_81_exciting_small,
			R.drawable.emo_im_82_big_smile_small,
			R.drawable.emo_im_83_haha_small,
			R.drawable.emo_im_84_victory_small,
			R.drawable.emo_im_85_red_heart_small,
			R.drawable.emo_im_86_amazing_small,
			R.drawable.emo_im_87_black_heart_small,
			R.drawable.emo_im_88_what_small,
			R.drawable.emo_im_89_bad_smile_small,
			R.drawable.emo_im_90_bad_egg_small,
			R.drawable.emo_im_91_grimace_small,
			R.drawable.emo_im_92_girl_small,
			R.drawable.emo_im_93_greedy_small,
			R.drawable.emo_im_94_anger_small,
			R.drawable.emo_im_95_eyes_droped_small,
			R.drawable.emo_im_96_happy_small,
			R.drawable.emo_im_97_horror_small,
			R.drawable.emo_im_98_money_small,
			R.drawable.emo_im_99_nothing_small,
			R.drawable.emo_im_100_nothing_to_say_small,
			R.drawable.emo_im_101_cry_small,
			R.drawable.emo_im_102_scorn_small,
			R.drawable.emo_im_103_secret_smile_small,
			R.drawable.emo_im_104_shame_small,
			R.drawable.emo_im_105_shocked_small,
			R.drawable.emo_im_106_super_man_small,
			R.drawable.emo_im_107_the_iron_man_small,
			R.drawable.emo_im_108_unhappy_small,
			R.drawable.emo_im_109_electric_shock_small,
			R.drawable.emo_im_110_beaten_small,
			R.drawable.emo_im_111_grin_small,
			R.drawable.emo_im_112_happy_small,
			R.drawable.emo_im_113_fake_smile_small,
			R.drawable.emo_im_114_in_love_small,
			R.drawable.emo_im_115_kiss_small,
			R.drawable.emo_im_116_straight_face_small,
			R.drawable.emo_im_117_meaw_small,
			R.drawable.emo_im_118_drunk_small,
			R.drawable.emo_im_119_x_x_small,
			R.drawable.emo_im_120_youre_kidding_right_small,
			R.drawable.emo_im_122_sweat_small,
			R.drawable.emo_im_123_nerd_small,
			R.drawable.emo_im_124_angry_small,
			R.drawable.emo_im_125_disappearing_small,
			R.drawable.emo_im_126_dizzy_small,
			R.drawable.emo_im_127_music_small,
			R.drawable.emo_im_128_evilish_small,
			R.drawable.emo_im_129_graffiti_small,
			R.drawable.emo_im_130_omg_small,
			R.drawable.emo_im_131_on_fire_small,
			R.drawable.emo_im_132_ouch_small,
			R.drawable.emo_im_133_angry_small,
			R.drawable.emo_im_134_serious_business_small,
			R.drawable.emo_im_135_sick_small,
			R.drawable.emo_im_136_slow_small,
			R.drawable.emo_im_137_snooty_small,
			R.drawable.emo_im_138_suspicious_small,
			R.drawable.emo_im_139_crying_small,
			R.drawable.emo_im_140_want_small,
			R.drawable.emo_im_141_we_all_gonna_die_small,
			R.drawable.emo_im_142_wut_small,
			R.drawable.emo_im_143_boo_small,
			R.drawable.emo_im_144_xd_small,
			R.drawable.emo_im_145_kaboom_small,
			R.drawable.emo_im_146_yarr_small,
			R.drawable.emo_im_147_ninja_small,
			R.drawable.emo_im_148_yuush_small,
			R.drawable.emo_im_149_brains_small,
			R.drawable.emo_im_150_sleeping_small,};

		public static final int BIGSMILE_01 = 1;
		public static final int HAPPY_02 = 2;
		public static final int LAUGH_03 = 3;
		public static final int SMILE_04 = 4;
		public static final int WINK_05 = 5;
		public static final int ADORE_06 = 6;
		public static final int KISS_07 = 7;
		public static final int KISSED_08 = 8;
		public static final int EXPRESSIONLESS_09 = 9;
		public static final int PUDENTLY_10 = 10;
		public static final int SATISFIED_11 = 11;
		public static final int GIGGLE_12 = 12;
		public static final int IMPISH_13 = 13;
		public static final int DISAPPOINTMENT_14 = 14;
		public static final int BEUPTONOGOOD_15 = 15;
		public static final int FRUSTRATED_16 = 16;
		public static final int SAD_17 = 17;
		public static final int SORRY_18 = 18;
		public static final int CRY_19 = 19;
		public static final int BORING_20 = 20;
		public static final int HUNGRY_21 = 21;
		public static final int SCARED_22 = 22;
		public static final int SHOCK_23 = 23;
		public static final int SWEAT_24 = 24;
		public static final int CRYING_25 = 25;
		public static final int LOL_26 = 26;
		public static final int WOO_27 = 27;
		public static final int SURPRISE_28 = 28;
		public static final int FROWN_29 = 29;
		public static final int ANGRY_30 = 30;
		public static final int WORNOUT_31 = 31;
		public static final int STOP_32 = 32;
		public static final int FURIOUS_33 = 33;
		public static final int SMOKING_34 = 34;
		public static final int HYSTERICAL_35 = 35;
		public static final int EXCLAMATION_36 = 36;
		public static final int QUESTION_37 = 37;
		public static final int SLEEP_38 = 38;
		public static final int AGGRESSIVE_39 = 39;
		public static final int BADLY_40 = 40;
		public static final int SINGING_41 = 41;
		public static final int BOMB_42 = 42;
		public static final int BEATEN_43 = 43;
		public static final int THUMBSDOWN_44 = 44;
		public static final int THUMBSUP_45 = 45;
		public static final int BEER_46 = 46;
		public static final int CALL_47 = 47;
		public static final int HI_48 = 48;
		public static final int HUG_49 = 49;
		public static final int FACEPALM_50 = 50;
		public static final int EASYMONEY_51 = 51;
		public static final int DIZZY_52 = 52;
		public static final int DISGUST_53 = 53;
		public static final int COCKTAIL_54 = 54;
		public static final int COFFEE_55 = 55;
		public static final int COLD_56 = 56;
		public static final int COOL_57 = 57;
		public static final int DESPAIR_58 = 58;
		public static final int HYPNOTIC_59 = 59;
		public static final int STARS_60 = 60;
		public static final int IDEA_61 = 61;
		public static final int MONOCLE_62 = 62;
		public static final int MOVIE_63 = 63;
		public static final int MUSIC_64 = 64;
		public static final int NERD_65 = 65;
		public static final int NINJA_66 = 66;
		public static final int PARTY_67 = 67;
		public static final int PIRATE_68 = 68;
		public static final int RAGE_69 = 69;
		public static final int ROSE_70 = 70;
		public static final int SICK_71 = 71;
		public static final int SNOTTY_72 = 72;
		public static final int STRESSED_73 = 73;
		public static final int STRUGGLE_74 = 74;
		public static final int STUDY_75 = 75;
		public static final int SWEETANGEL_76 = 76;
		public static final int THINKING_77 = 77;
		public static final int WAITING_78 = 78;
		public static final int WHISTLING_79 = 79;
		public static final int YAWN_80 = 80;
		public static final int EXCITING_81 = 81;
		public static final int BIGSMILE_82 = 82;
		public static final int HAHA_83 = 83;
		public static final int VICTORY_84 = 84;
		public static final int REDHEART_85 = 85;
		public static final int AMAZING_86 = 86;
		public static final int BLACKHEART_87 = 87;
		public static final int WHAT_88 = 88;
		public static final int BADSMILE_89 = 89;
		public static final int BADEGG_90 = 90;
		public static final int GRIMACE_91 = 91;
		public static final int GIRL_92 = 92;
		public static final int GREEDY_93 = 93;
		public static final int ANGER_94 = 94;
		public static final int EYESDROPED_95 = 95;
		public static final int HAPPY_96 = 96;
		public static final int HORROR_97 = 97;
		public static final int MONEY_98 = 98;
		public static final int NOTHING_99 = 99;
		public static final int NOTHINGTOSAY_100 = 100;
		public static final int CRY_101 = 101;
		public static final int SCORN_102 = 102;
		public static final int SECRETSMILE_103 = 103;
		public static final int SHAME_104 = 104;
		public static final int SHOCKED_105 = 105;
		public static final int SUPERMAN_106 = 106;
		public static final int THEIRONMAN_107 = 107;
		public static final int UNHAPPY_108 = 108;
		public static final int ELECTRICSHOCK_109 = 109;
		public static final int BEATEN_110 = 110;
		public static final int GRIN_111 = 111;
		public static final int HAPPY_112 = 112;
		public static final int FAKESMILE_113 = 113;
		public static final int INLOVE_114 = 114;
		public static final int KISS_115 = 115;
		public static final int STRAIGHTFACE_116 = 116;
		public static final int MEAW_117 = 117;
		public static final int DRUNK_118 = 118;
		public static final int XX_119 = 119;
		public static final int YOUREKIDDINGRIGHT_120 = 120;
		public static final int SWEAT_122 = 121;
		public static final int NERD_123 = 122;
		public static final int ANGRY_124 = 123;
		public static final int DISAPPEARING_125 = 124;
		public static final int DIZZY_126 = 125;
		public static final int MUSIC_127 = 126;
		public static final int EVILISH_128 = 127;
		public static final int GRAFFITI_129 = 128;
		public static final int OMG_130 = 129;
		public static final int ONFIRE_131 = 130;
		public static final int OUCH_132 = 131;
		public static final int ANGRY_133 = 132;
		public static final int SERIOUSBUSINESS_134 = 133;
		public static final int SICK_135 = 134;
		public static final int SLOW_136 = 135;
		public static final int SNOOTY_137 = 136;
		public static final int SUSPICIOUS_138 = 137;
		public static final int CRYING_139 = 138;
		public static final int WANT_140 = 139;
		public static final int WEALLGONNADIE_141 = 140;
		public static final int WUT_142 = 141;
		public static final int BOO_143 = 142;
		public static final int XD_144 = 143;
		public static final int KABOOM_145 = 144;
		public static final int YARR_146 = 145;
		public static final int NINJA_147 = 146;
		public static final int YUUSH_148 = 147;
		public static final int BRAINS_149 = 148;
		public static final int SLEEPING_150 = 149;

		public static int getSmileyResource(int which)
		{
			return sIconIds[which - 1];
		}

		public static int getSmallSmileyResource(int which)
		{
			return sIconIdsSmall[which - 1];
		}
	}

	// NOTE: if you change anything about this array, you must make the corresponding change
	// to the string arrays: default_smiley_texts and default_smiley_names in res/values/arrays.xml
	public static final int[] DEFAULT_SMILEY_RES_IDS = {
		Smileys.getSmileyResource(Smileys.BIGSMILE_01),
		Smileys.getSmileyResource(Smileys.HAPPY_02),
		Smileys.getSmileyResource(Smileys.LAUGH_03),
		Smileys.getSmileyResource(Smileys.SMILE_04),
		Smileys.getSmileyResource(Smileys.WINK_05),
		Smileys.getSmileyResource(Smileys.ADORE_06),
		Smileys.getSmileyResource(Smileys.KISS_07),
		Smileys.getSmileyResource(Smileys.KISSED_08),
		Smileys.getSmileyResource(Smileys.EXPRESSIONLESS_09),
		Smileys.getSmileyResource(Smileys.PUDENTLY_10),
		Smileys.getSmileyResource(Smileys.SATISFIED_11),
		Smileys.getSmileyResource(Smileys.GIGGLE_12),
		Smileys.getSmileyResource(Smileys.IMPISH_13),
		Smileys.getSmileyResource(Smileys.DISAPPOINTMENT_14),
		Smileys.getSmileyResource(Smileys.BEUPTONOGOOD_15),
		Smileys.getSmileyResource(Smileys.FRUSTRATED_16),
		Smileys.getSmileyResource(Smileys.SAD_17),
		Smileys.getSmileyResource(Smileys.SORRY_18),
		Smileys.getSmileyResource(Smileys.CRY_19),
		Smileys.getSmileyResource(Smileys.BORING_20),
		Smileys.getSmileyResource(Smileys.HUNGRY_21),
		Smileys.getSmileyResource(Smileys.SCARED_22),
		Smileys.getSmileyResource(Smileys.SHOCK_23),
		Smileys.getSmileyResource(Smileys.SWEAT_24),
		Smileys.getSmileyResource(Smileys.CRYING_25),
		Smileys.getSmileyResource(Smileys.LOL_26),
		Smileys.getSmileyResource(Smileys.WOO_27),
		Smileys.getSmileyResource(Smileys.SURPRISE_28),
		Smileys.getSmileyResource(Smileys.FROWN_29),
		Smileys.getSmileyResource(Smileys.ANGRY_30),
		Smileys.getSmileyResource(Smileys.WORNOUT_31),
		Smileys.getSmileyResource(Smileys.STOP_32),
		Smileys.getSmileyResource(Smileys.FURIOUS_33),
		Smileys.getSmileyResource(Smileys.SMOKING_34),
		Smileys.getSmileyResource(Smileys.HYSTERICAL_35),
		Smileys.getSmileyResource(Smileys.EXCLAMATION_36),
		Smileys.getSmileyResource(Smileys.QUESTION_37),
		Smileys.getSmileyResource(Smileys.SLEEP_38),
		Smileys.getSmileyResource(Smileys.AGGRESSIVE_39),
		Smileys.getSmileyResource(Smileys.BADLY_40),
		Smileys.getSmileyResource(Smileys.SINGING_41),
		Smileys.getSmileyResource(Smileys.BOMB_42),
		Smileys.getSmileyResource(Smileys.BEATEN_43),
		Smileys.getSmileyResource(Smileys.THUMBSDOWN_44),
		Smileys.getSmileyResource(Smileys.THUMBSUP_45),
		Smileys.getSmileyResource(Smileys.BEER_46),
		Smileys.getSmileyResource(Smileys.CALL_47),
		Smileys.getSmileyResource(Smileys.HI_48),
		Smileys.getSmileyResource(Smileys.HUG_49),
		Smileys.getSmileyResource(Smileys.FACEPALM_50),
		Smileys.getSmileyResource(Smileys.EASYMONEY_51),
		Smileys.getSmileyResource(Smileys.DIZZY_52),
		Smileys.getSmileyResource(Smileys.DISGUST_53),
		Smileys.getSmileyResource(Smileys.COCKTAIL_54),
		Smileys.getSmileyResource(Smileys.COFFEE_55),
		Smileys.getSmileyResource(Smileys.COLD_56),
		Smileys.getSmileyResource(Smileys.COOL_57),
		Smileys.getSmileyResource(Smileys.DESPAIR_58),
		Smileys.getSmileyResource(Smileys.HYPNOTIC_59),
		Smileys.getSmileyResource(Smileys.STARS_60),
		Smileys.getSmileyResource(Smileys.IDEA_61),
		Smileys.getSmileyResource(Smileys.MONOCLE_62),
		Smileys.getSmileyResource(Smileys.MOVIE_63),
		Smileys.getSmileyResource(Smileys.MUSIC_64),
		Smileys.getSmileyResource(Smileys.NERD_65),
		Smileys.getSmileyResource(Smileys.NINJA_66),
		Smileys.getSmileyResource(Smileys.PARTY_67),
		Smileys.getSmileyResource(Smileys.PIRATE_68),
		Smileys.getSmileyResource(Smileys.RAGE_69),
		Smileys.getSmileyResource(Smileys.ROSE_70),
		Smileys.getSmileyResource(Smileys.SICK_71),
		Smileys.getSmileyResource(Smileys.SNOTTY_72),
		Smileys.getSmileyResource(Smileys.STRESSED_73),
		Smileys.getSmileyResource(Smileys.STRUGGLE_74),
		Smileys.getSmileyResource(Smileys.STUDY_75),
		Smileys.getSmileyResource(Smileys.SWEETANGEL_76),
		Smileys.getSmileyResource(Smileys.THINKING_77),
		Smileys.getSmileyResource(Smileys.WAITING_78),
		Smileys.getSmileyResource(Smileys.WHISTLING_79),
		Smileys.getSmileyResource(Smileys.YAWN_80),
		Smileys.getSmileyResource(Smileys.EXCITING_81),
		Smileys.getSmileyResource(Smileys.BIGSMILE_82),
		Smileys.getSmileyResource(Smileys.HAHA_83),
		Smileys.getSmileyResource(Smileys.VICTORY_84),
		Smileys.getSmileyResource(Smileys.REDHEART_85),
		Smileys.getSmileyResource(Smileys.AMAZING_86),
		Smileys.getSmileyResource(Smileys.BLACKHEART_87),
		Smileys.getSmileyResource(Smileys.WHAT_88),
		Smileys.getSmileyResource(Smileys.BADSMILE_89),
		Smileys.getSmileyResource(Smileys.BADEGG_90),
		Smileys.getSmileyResource(Smileys.GRIMACE_91),
		Smileys.getSmileyResource(Smileys.GIRL_92),
		Smileys.getSmileyResource(Smileys.GREEDY_93),
		Smileys.getSmileyResource(Smileys.ANGER_94),
		Smileys.getSmileyResource(Smileys.EYESDROPED_95),
		Smileys.getSmileyResource(Smileys.HAPPY_96),
		Smileys.getSmileyResource(Smileys.HORROR_97),
		Smileys.getSmileyResource(Smileys.MONEY_98),
		Smileys.getSmileyResource(Smileys.NOTHING_99),
		Smileys.getSmileyResource(Smileys.NOTHINGTOSAY_100),
		Smileys.getSmileyResource(Smileys.CRY_101),
		Smileys.getSmileyResource(Smileys.SCORN_102),
		Smileys.getSmileyResource(Smileys.SECRETSMILE_103),
		Smileys.getSmileyResource(Smileys.SHAME_104),
		Smileys.getSmileyResource(Smileys.SHOCKED_105),
		Smileys.getSmileyResource(Smileys.SUPERMAN_106),
		Smileys.getSmileyResource(Smileys.THEIRONMAN_107),
		Smileys.getSmileyResource(Smileys.UNHAPPY_108),
		Smileys.getSmileyResource(Smileys.ELECTRICSHOCK_109),
		Smileys.getSmileyResource(Smileys.BEATEN_110),
		Smileys.getSmileyResource(Smileys.GRIN_111),
		Smileys.getSmileyResource(Smileys.HAPPY_112),
		Smileys.getSmileyResource(Smileys.FAKESMILE_113),
		Smileys.getSmileyResource(Smileys.INLOVE_114),
		Smileys.getSmileyResource(Smileys.KISS_115),
		Smileys.getSmileyResource(Smileys.STRAIGHTFACE_116),
		Smileys.getSmileyResource(Smileys.MEAW_117),
		Smileys.getSmileyResource(Smileys.DRUNK_118),
		Smileys.getSmileyResource(Smileys.XX_119),
		Smileys.getSmileyResource(Smileys.YOUREKIDDINGRIGHT_120),
		Smileys.getSmileyResource(Smileys.SWEAT_122),
		Smileys.getSmileyResource(Smileys.NERD_123),
		Smileys.getSmileyResource(Smileys.ANGRY_124),
		Smileys.getSmileyResource(Smileys.DISAPPEARING_125),
		Smileys.getSmileyResource(Smileys.DIZZY_126),
		Smileys.getSmileyResource(Smileys.MUSIC_127),
		Smileys.getSmileyResource(Smileys.EVILISH_128),
		Smileys.getSmileyResource(Smileys.GRAFFITI_129),
		Smileys.getSmileyResource(Smileys.OMG_130),
		Smileys.getSmileyResource(Smileys.ONFIRE_131),
		Smileys.getSmileyResource(Smileys.OUCH_132),
		Smileys.getSmileyResource(Smileys.ANGRY_133),
		Smileys.getSmileyResource(Smileys.SERIOUSBUSINESS_134),
		Smileys.getSmileyResource(Smileys.SICK_135),
		Smileys.getSmileyResource(Smileys.SLOW_136),
		Smileys.getSmileyResource(Smileys.SNOOTY_137),
		Smileys.getSmileyResource(Smileys.SUSPICIOUS_138),
		Smileys.getSmileyResource(Smileys.CRYING_139),
		Smileys.getSmileyResource(Smileys.WANT_140),
		Smileys.getSmileyResource(Smileys.WEALLGONNADIE_141),
		Smileys.getSmileyResource(Smileys.WUT_142),
		Smileys.getSmileyResource(Smileys.BOO_143),
		Smileys.getSmileyResource(Smileys.XD_144),
		Smileys.getSmileyResource(Smileys.KABOOM_145),
		Smileys.getSmileyResource(Smileys.YARR_146),
		Smileys.getSmileyResource(Smileys.NINJA_147),
		Smileys.getSmileyResource(Smileys.YUUSH_148),
		Smileys.getSmileyResource(Smileys.BRAINS_149),
		Smileys.getSmileyResource(Smileys.SLEEPING_150)
	};

	public static final int[] DEFAULT_SMALL_SMILEY_RES_IDS = {
		Smileys.getSmallSmileyResource(Smileys.BIGSMILE_01),
		Smileys.getSmallSmileyResource(Smileys.HAPPY_02),
		Smileys.getSmallSmileyResource(Smileys.LAUGH_03),
		Smileys.getSmallSmileyResource(Smileys.SMILE_04),
		Smileys.getSmallSmileyResource(Smileys.WINK_05),
		Smileys.getSmallSmileyResource(Smileys.ADORE_06),
		Smileys.getSmallSmileyResource(Smileys.KISS_07),
		Smileys.getSmallSmileyResource(Smileys.KISSED_08),
		Smileys.getSmallSmileyResource(Smileys.EXPRESSIONLESS_09),
		Smileys.getSmallSmileyResource(Smileys.PUDENTLY_10),
		Smileys.getSmallSmileyResource(Smileys.SATISFIED_11),
		Smileys.getSmallSmileyResource(Smileys.GIGGLE_12),
		Smileys.getSmallSmileyResource(Smileys.IMPISH_13),
		Smileys.getSmallSmileyResource(Smileys.DISAPPOINTMENT_14),
		Smileys.getSmallSmileyResource(Smileys.BEUPTONOGOOD_15),
		Smileys.getSmallSmileyResource(Smileys.FRUSTRATED_16),
		Smileys.getSmallSmileyResource(Smileys.SAD_17),
		Smileys.getSmallSmileyResource(Smileys.SORRY_18),
		Smileys.getSmallSmileyResource(Smileys.CRY_19),
		Smileys.getSmallSmileyResource(Smileys.BORING_20),
		Smileys.getSmallSmileyResource(Smileys.HUNGRY_21),
		Smileys.getSmallSmileyResource(Smileys.SCARED_22),
		Smileys.getSmallSmileyResource(Smileys.SHOCK_23),
		Smileys.getSmallSmileyResource(Smileys.SWEAT_24),
		Smileys.getSmallSmileyResource(Smileys.CRYING_25),
		Smileys.getSmallSmileyResource(Smileys.LOL_26),
		Smileys.getSmallSmileyResource(Smileys.WOO_27),
		Smileys.getSmallSmileyResource(Smileys.SURPRISE_28),
		Smileys.getSmallSmileyResource(Smileys.FROWN_29),
		Smileys.getSmallSmileyResource(Smileys.ANGRY_30),
		Smileys.getSmallSmileyResource(Smileys.WORNOUT_31),
		Smileys.getSmallSmileyResource(Smileys.STOP_32),
		Smileys.getSmallSmileyResource(Smileys.FURIOUS_33),
		Smileys.getSmallSmileyResource(Smileys.SMOKING_34),
		Smileys.getSmallSmileyResource(Smileys.HYSTERICAL_35),
		Smileys.getSmallSmileyResource(Smileys.EXCLAMATION_36),
		Smileys.getSmallSmileyResource(Smileys.QUESTION_37),
		Smileys.getSmallSmileyResource(Smileys.SLEEP_38),
		Smileys.getSmallSmileyResource(Smileys.AGGRESSIVE_39),
		Smileys.getSmallSmileyResource(Smileys.BADLY_40),
		Smileys.getSmallSmileyResource(Smileys.SINGING_41),
		Smileys.getSmallSmileyResource(Smileys.BOMB_42),
		Smileys.getSmallSmileyResource(Smileys.BEATEN_43),
		Smileys.getSmallSmileyResource(Smileys.THUMBSDOWN_44),
		Smileys.getSmallSmileyResource(Smileys.THUMBSUP_45),
		Smileys.getSmallSmileyResource(Smileys.BEER_46),
		Smileys.getSmallSmileyResource(Smileys.CALL_47),
		Smileys.getSmallSmileyResource(Smileys.HI_48),
		Smileys.getSmallSmileyResource(Smileys.HUG_49),
		Smileys.getSmallSmileyResource(Smileys.FACEPALM_50),
		Smileys.getSmallSmileyResource(Smileys.EASYMONEY_51),
		Smileys.getSmallSmileyResource(Smileys.DIZZY_52),
		Smileys.getSmallSmileyResource(Smileys.DISGUST_53),
		Smileys.getSmallSmileyResource(Smileys.COCKTAIL_54),
		Smileys.getSmallSmileyResource(Smileys.COFFEE_55),
		Smileys.getSmallSmileyResource(Smileys.COLD_56),
		Smileys.getSmallSmileyResource(Smileys.COOL_57),
		Smileys.getSmallSmileyResource(Smileys.DESPAIR_58),
		Smileys.getSmallSmileyResource(Smileys.HYPNOTIC_59),
		Smileys.getSmallSmileyResource(Smileys.STARS_60),
		Smileys.getSmallSmileyResource(Smileys.IDEA_61),
		Smileys.getSmallSmileyResource(Smileys.MONOCLE_62),
		Smileys.getSmallSmileyResource(Smileys.MOVIE_63),
		Smileys.getSmallSmileyResource(Smileys.MUSIC_64),
		Smileys.getSmallSmileyResource(Smileys.NERD_65),
		Smileys.getSmallSmileyResource(Smileys.NINJA_66),
		Smileys.getSmallSmileyResource(Smileys.PARTY_67),
		Smileys.getSmallSmileyResource(Smileys.PIRATE_68),
		Smileys.getSmallSmileyResource(Smileys.RAGE_69),
		Smileys.getSmallSmileyResource(Smileys.ROSE_70),
		Smileys.getSmallSmileyResource(Smileys.SICK_71),
		Smileys.getSmallSmileyResource(Smileys.SNOTTY_72),
		Smileys.getSmallSmileyResource(Smileys.STRESSED_73),
		Smileys.getSmallSmileyResource(Smileys.STRUGGLE_74),
		Smileys.getSmallSmileyResource(Smileys.STUDY_75),
		Smileys.getSmallSmileyResource(Smileys.SWEETANGEL_76),
		Smileys.getSmallSmileyResource(Smileys.THINKING_77),
		Smileys.getSmallSmileyResource(Smileys.WAITING_78),
		Smileys.getSmallSmileyResource(Smileys.WHISTLING_79),
		Smileys.getSmallSmileyResource(Smileys.YAWN_80),
		Smileys.getSmallSmileyResource(Smileys.EXCITING_81),
		Smileys.getSmallSmileyResource(Smileys.BIGSMILE_82),
		Smileys.getSmallSmileyResource(Smileys.HAHA_83),
		Smileys.getSmallSmileyResource(Smileys.VICTORY_84),
		Smileys.getSmallSmileyResource(Smileys.REDHEART_85),
		Smileys.getSmallSmileyResource(Smileys.AMAZING_86),
		Smileys.getSmallSmileyResource(Smileys.BLACKHEART_87),
		Smileys.getSmallSmileyResource(Smileys.WHAT_88),
		Smileys.getSmallSmileyResource(Smileys.BADSMILE_89),
		Smileys.getSmallSmileyResource(Smileys.BADEGG_90),
		Smileys.getSmallSmileyResource(Smileys.GRIMACE_91),
		Smileys.getSmallSmileyResource(Smileys.GIRL_92),
		Smileys.getSmallSmileyResource(Smileys.GREEDY_93),
		Smileys.getSmallSmileyResource(Smileys.ANGER_94),
		Smileys.getSmallSmileyResource(Smileys.EYESDROPED_95),
		Smileys.getSmallSmileyResource(Smileys.HAPPY_96),
		Smileys.getSmallSmileyResource(Smileys.HORROR_97),
		Smileys.getSmallSmileyResource(Smileys.MONEY_98),
		Smileys.getSmallSmileyResource(Smileys.NOTHING_99),
		Smileys.getSmallSmileyResource(Smileys.NOTHINGTOSAY_100),
		Smileys.getSmallSmileyResource(Smileys.CRY_101),
		Smileys.getSmallSmileyResource(Smileys.SCORN_102),
		Smileys.getSmallSmileyResource(Smileys.SECRETSMILE_103),
		Smileys.getSmallSmileyResource(Smileys.SHAME_104),
		Smileys.getSmallSmileyResource(Smileys.SHOCKED_105),
		Smileys.getSmallSmileyResource(Smileys.SUPERMAN_106),
		Smileys.getSmallSmileyResource(Smileys.THEIRONMAN_107),
		Smileys.getSmallSmileyResource(Smileys.UNHAPPY_108),
		Smileys.getSmallSmileyResource(Smileys.ELECTRICSHOCK_109),
		Smileys.getSmallSmileyResource(Smileys.BEATEN_110),
		Smileys.getSmallSmileyResource(Smileys.GRIN_111),
		Smileys.getSmallSmileyResource(Smileys.HAPPY_112),
		Smileys.getSmallSmileyResource(Smileys.FAKESMILE_113),
		Smileys.getSmallSmileyResource(Smileys.INLOVE_114),
		Smileys.getSmallSmileyResource(Smileys.KISS_115),
		Smileys.getSmallSmileyResource(Smileys.STRAIGHTFACE_116),
		Smileys.getSmallSmileyResource(Smileys.MEAW_117),
		Smileys.getSmallSmileyResource(Smileys.DRUNK_118),
		Smileys.getSmallSmileyResource(Smileys.XX_119),
		Smileys.getSmallSmileyResource(Smileys.YOUREKIDDINGRIGHT_120),
		Smileys.getSmallSmileyResource(Smileys.SWEAT_122),
		Smileys.getSmallSmileyResource(Smileys.NERD_123),
		Smileys.getSmallSmileyResource(Smileys.ANGRY_124),
		Smileys.getSmallSmileyResource(Smileys.DISAPPEARING_125),
		Smileys.getSmallSmileyResource(Smileys.DIZZY_126),
		Smileys.getSmallSmileyResource(Smileys.MUSIC_127),
		Smileys.getSmallSmileyResource(Smileys.EVILISH_128),
		Smileys.getSmallSmileyResource(Smileys.GRAFFITI_129),
		Smileys.getSmallSmileyResource(Smileys.OMG_130),
		Smileys.getSmallSmileyResource(Smileys.ONFIRE_131),
		Smileys.getSmallSmileyResource(Smileys.OUCH_132),
		Smileys.getSmallSmileyResource(Smileys.ANGRY_133),
		Smileys.getSmallSmileyResource(Smileys.SERIOUSBUSINESS_134),
		Smileys.getSmallSmileyResource(Smileys.SICK_135),
		Smileys.getSmallSmileyResource(Smileys.SLOW_136),
		Smileys.getSmallSmileyResource(Smileys.SNOOTY_137),
		Smileys.getSmallSmileyResource(Smileys.SUSPICIOUS_138),
		Smileys.getSmallSmileyResource(Smileys.CRYING_139),
		Smileys.getSmallSmileyResource(Smileys.WANT_140),
		Smileys.getSmallSmileyResource(Smileys.WEALLGONNADIE_141),
		Smileys.getSmallSmileyResource(Smileys.WUT_142),
		Smileys.getSmallSmileyResource(Smileys.BOO_143),
		Smileys.getSmallSmileyResource(Smileys.XD_144),
		Smileys.getSmallSmileyResource(Smileys.KABOOM_145),
		Smileys.getSmallSmileyResource(Smileys.YARR_146),
		Smileys.getSmallSmileyResource(Smileys.NINJA_147),
		Smileys.getSmallSmileyResource(Smileys.YUUSH_148),
		Smileys.getSmallSmileyResource(Smileys.BRAINS_149),
		Smileys.getSmallSmileyResource(Smileys.SLEEPING_150)
	};

	public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;

	/**
	 * Builds the hashtable we use for mapping the string version of a smiley (e.g. ":-)") to a resource ID for the icon version.
	 */
	private HashMap<String, Integer> buildSmileyToRes()
	{
		if (DEFAULT_SMILEY_RES_IDS.length != mSmileyTexts.length)
		{
			// Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
			// and failed to update arrays.xml
			throw new IllegalStateException("Smiley resource ID/text mismatch");
		}

		HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mSmileyTexts.length);
		for (int i = 0; i < mSmileyTexts.length; i++)
		{
			smileyToRes.put(mSmileyTexts[i], DEFAULT_SMILEY_RES_IDS[i]);
		}

		return smileyToRes;
	}

	private HashMap<String, Integer> buildSmallSmileyToRes()
	{
		if (DEFAULT_SMALL_SMILEY_RES_IDS.length != mSmileyTexts.length)
		{
			// Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
			// and failed to update arrays.xml
			throw new IllegalStateException("Small Smiley resource ID/text mismatch");
		}

		HashMap<String, Integer> smallSmileyToRes = new HashMap<String, Integer>(mSmileyTexts.length);
		for (int i = 0; i < mSmileyTexts.length; i++)
		{
			smallSmileyToRes.put(mSmileyTexts[i], DEFAULT_SMALL_SMILEY_RES_IDS[i]);
		}

		return smallSmileyToRes;
	}

	/**
	 * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
	 */
	private Pattern buildPattern()
	{
		StringBuilder patternString = new StringBuilder();

		// Build a regex that looks like (:-)|:-(|...), but escaping the smilies
		// properly so they will be interpreted literally by the regex matcher.
		patternString.append('(');
		for (String s : mSmileyTexts)
		{
			patternString.append(Pattern.quote(s));
			patternString.append('|');
		}
		// Replace the extra '|' with a ')'
		patternString.replace(patternString.length() - 1, patternString.length(), ")");

		return Pattern.compile(patternString.toString());
	}

	/**
	 * Adds ImageSpans to a CharSequence that replace textual emoticons such as :-) with a graphical version.
	 * 
	 * @param text
	 *            A CharSequence possibly containing emoticons
	 * @param showSmallIcon
	 * 			  A boolean value which indicates whether to display the large or the small icon
	 * @return A CharSequence annotated with ImageSpans covering any recognized emoticons.
	 */
	public CharSequence addSmileySpans(CharSequence text, boolean showSmallIcon)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder(text);

		Matcher matcher = mPattern.matcher(text);
		int count = 0;
		while (matcher.find() && (count < MAX_EMOTICONS))
		{
			count++;
			int resId = showSmallIcon ? mSmallSmileyToRes.get(matcher.group()) : mSmileyToRes.get(matcher.group());
			builder.setSpan(new ImageSpan(mContext, resId), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return builder;
	}

	/**
	 * Adds an emoticon image to the compose box 
	 * @param composeBox: A reference to the text box in which the emoticon will be shown.
	 * @param whichEmoticon: Integer value of the emoticon which is to be shown.
	 */
	public void addSmiley(EditText composeBox, int whichEmoticon)
	{
		Editable text = composeBox.getText();
		text.append(mSmileyTexts[whichEmoticon]);

		composeBox.setText(addSmileySpans(text, false));
		composeBox.setSelection(composeBox.length());
	}

	/**
	 * Used for adding smileys to the compose box while the user is typing.
	 * @param editable: this should be the same editable passed to us in the afterTextChanged method of the TextWatcher.
	 */
	public void addSmileyToEditable(Editable editable)
	{
		Matcher matcher = mPattern.matcher(editable);
		int count = 0;
		while (matcher.find() && (count < MAX_EMOTICONS))
		{
			count++;
			int resId = mSmileyToRes.get(matcher.group());
			editable.setSpan(new ImageSpan(mContext, resId), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

	}
}
