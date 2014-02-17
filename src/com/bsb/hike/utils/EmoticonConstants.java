package com.bsb.hike.utils;

import java.util.HashMap;
import java.util.Map;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;

public class EmoticonConstants
{
	public static final String[] mEmojiUnicodes = { "\uD83D\uDE04", "\uD83D\uDE0A", "\uD83D\uDE03", "\u263A", "\uD83D\uDE09", "\uD83D\uDE0D", "\uD83D\uDE18", "\uD83D\uDE1A",
			"\uD83D\uDE33", "\uD83D\uDE0C", "\uD83D\uDE01", "\uD83D\uDE1C", "\uD83D\uDE1D", "\uD83D\uDE12", "\uD83D\uDE0F", "\uD83D\uDE13", "\uD83D\uDE14", "\uD83D\uDE1E",
			"\uD83D\uDE16", "\uD83D\uDE25", "\uD83D\uDE30", "\uD83D\uDE28", "\uD83D\uDE23", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE02", "\uD83D\uDE32", "\uD83D\uDE31",
			"\uD83D\uDE20", "\uD83D\uDE21", "\uD83D\uDE2A", "\uD83D\uDE37", "\uD83D\uDC7F", "\uD83D\uDC7D", "\uD83D\uDC9B", "\uD83D\uDC99", "\uD83D\uDC9C", "\uD83D\uDC97",
			"\uD83D\uDC9A", "\u2764", "\uD83D\uDC94", "\uD83D\uDC93", "\uD83D\uDC98", "\u2728", "\uD83C\uDF1F", "\uD83D\uDCA2", "\u2755", "\u2754", "\uD83D\uDCA4", "\uD83D\uDCA8",
			"\uD83D\uDCA6", "\uD83C\uDFB6", "\uD83C\uDFB5", "\uD83D\uDD25", "\uD83D\uDCA9", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4C", "\uD83D\uDC4A", "\u270A", "\u270C",
			"\uD83D\uDC4B", "\u270B", "\uD83D\uDC50", "\uD83D\uDC46", "\uD83D\uDC47", "\uD83D\uDC49", "\uD83D\uDC48", "\uD83D\uDE4C", "\uD83D\uDE4F", "\u261D", "\uD83D\uDC4F",
			"\uD83D\uDCAA", "\uD83D\uDEB6", "\uD83C\uDFC3", "\uD83D\uDC6B", "\uD83D\uDC83", "\uD83D\uDC6F", "\uD83D\uDE46", "\uD83D\uDE45", "\uD83D\uDC81", "\uD83D\uDE47",
			"\uD83D\uDC8F", "\uD83D\uDC91", "\uD83D\uDC86", "\uD83D\uDC87", "\uD83D\uDC85", "\uD83D\uDC66", "\uD83D\uDC67", "\uD83D\uDC69", "\uD83D\uDC68", "\uD83D\uDC76",
			"\uD83D\uDC75", "\uD83D\uDC74", "\uD83D\uDC71", "\uD83D\uDC72", "\uD83D\uDC73", "\uD83D\uDC77", "\uD83D\uDC6E", "\uD83D\uDC7C", "\uD83D\uDC78", "\uD83D\uDC82",
			"\uD83D\uDC80", "\uD83D\uDC63", "\uD83D\uDC8B", "\uD83D\uDC44", "\uD83D\uDC42", "\uD83D\uDC40", "\uD83D\uDC43", "\u2600", "\u2614", "\u2601", "\u26C4", "\uD83C\uDF19",
			"\u26A1", "\uD83C\uDF00", "\uD83C\uDF0A", "\uD83D\uDC31", "\uD83D\uDC36", "\uD83D\uDC2D", "\uD83D\uDC39", "\uD83D\uDC30", "\uD83D\uDC3A", "\uD83D\uDC38",
			"\uD83D\uDC2F", "\uD83D\uDC28", "\uD83D\uDC3B", "\uD83D\uDC37", "\uD83D\uDC2E", "\uD83D\uDC17", "\uD83D\uDC35", "\uD83D\uDC12", "\uD83D\uDC34", "\uD83D\uDC0E",
			"\uD83D\uDC2B", "\uD83D\uDC11", "\uD83D\uDC18", "\uD83D\uDC0D", "\uD83D\uDC26", "\uD83D\uDC24", "\uD83D\uDC14", "\uD83D\uDC27", "\uD83D\uDC1B", "\uD83D\uDC19",
			"\uD83D\uDC20", "\uD83D\uDC1F", "\uD83D\uDC33", "\uD83D\uDC2C", "\uD83D\uDC90", "\uD83C\uDF38", "\uD83C\uDF37", "\uD83C\uDF40", "\uD83C\uDF39", "\uD83C\uDF3B",
			"\uD83C\uDF3A", "\uD83C\uDF41", "\uD83C\uDF43", "\uD83C\uDF42", "\uD83C\uDF34", "\uD83C\uDF35", "\uD83C\uDF3E", "\uD83D\uDC1A", "\uD83C\uDF8D", "\uD83D\uDC9D",
			"\uD83C\uDF8E", "\uD83C\uDF92", "\uD83C\uDF93", "\uD83C\uDF8F", "\uD83C\uDF86", "\uD83C\uDF87", "\uD83C\uDF90", "\uD83C\uDF91", "\uD83C\uDF83", "\uD83D\uDC7B",
			"\uD83C\uDF85", "\uD83C\uDF84", "\uD83C\uDF81", "\uD83D\uDD14", "\uD83C\uDF89", "\uD83C\uDF88", "\uD83D\uDCBF", "\uD83D\uDCC0", "\uD83D\uDCF7", "\uD83C\uDFA5",
			"\uD83D\uDCBB", "\uD83D\uDCFA", "\uD83D\uDCF1", "\uD83D\uDCE0", "\u260E", "\uD83D\uDCBD", "\uD83D\uDCFC", "\uD83D\uDD0A", "\uD83D\uDCE2", "\uD83D\uDCE3",
			"\uD83D\uDCFB", "\uD83D\uDCE1", "\u27BF", "\uD83D\uDD0D", "\uD83D\uDD13", "\uD83D\uDD12", "\uD83D\uDD11", "\u2702", "\uD83D\uDD28", "\uD83D\uDCA1", "\uD83D\uDCF2",
			"\uD83D\uDCE9", "\uD83D\uDCEB", "\uD83D\uDCEE", "\uD83D\uDEC0", "\uD83D\uDEBD", "\uD83D\uDCBA", "\uD83D\uDCB0", "\uD83D\uDD31", "\uD83D\uDEAC", "\uD83D\uDCA3",
			"\uD83D\uDD2B", "\uD83D\uDC8A", "\uD83D\uDC89", "\uD83C\uDFC8", "\uD83C\uDFC0", "\u26BD", "\u26BE", "\uD83C\uDFBE", "\u26F3", "\uD83C\uDFB1", "\uD83C\uDFCA",
			"\uD83C\uDFC4", "\uD83C\uDFBF", "\u2660", "\u2665", "\u2663", "\u2666", "\uD83C\uDFC6", "\uD83D\uDC7E", "\uD83C\uDFAF", "\uD83C\uDC04", "\uD83C\uDFAC", "\uD83D\uDCDD",
			"\uD83D\uDCD6", "\uD83C\uDFA8", "\uD83C\uDFA4", "\uD83C\uDFA7", "\uD83C\uDFBA", "\uD83C\uDFB7", "\uD83C\uDFB8", "\u303D", "\uD83D\uDC5F", "\uD83D\uDC61",
			"\uD83D\uDC60", "\uD83D\uDC62", "\uD83D\uDC55", "\uD83D\uDC54", "\uD83D\uDC57", "\uD83D\uDC58", "\uD83D\uDC59", "\uD83C\uDF80", "\uD83C\uDFA9", "\uD83D\uDC51",
			"\uD83D\uDC52", "\uD83C\uDF02", "\uD83D\uDCBC", "\uD83D\uDC5C", "\uD83D\uDC84", "\uD83D\uDC8D", "\uD83D\uDC8E", "\u2615", "\uD83C\uDF75", "\uD83C\uDF7A",
			"\uD83C\uDF7B", "\uD83C\uDF78", "\uD83C\uDF76", "\uD83C\uDF74", "\uD83C\uDF54", "\uD83C\uDF5F", "\uD83C\uDF5D", "\uD83C\uDF5B", "\uD83C\uDF71", "\uD83C\uDF63",
			"\uD83C\uDF59", "\uD83C\uDF58", "\uD83C\uDF5A", "\uD83C\uDF5C", "\uD83C\uDF72", "\uD83C\uDF5E", "\uD83C\uDF73", "\uD83C\uDF62", "\uD83C\uDF61", "\uD83C\uDF66",
			"\uD83C\uDF67", "\uD83C\uDF82", "\uD83C\uDF70", "\uD83C\uDF4E", "\uD83C\uDF4A", "\uD83C\uDF49", "\uD83C\uDF53", "\uD83C\uDF46", "\uD83C\uDF45", "\uD83C\uDFE0",
			"\uD83C\uDFEB", "\uD83C\uDFE2", "\uD83C\uDFE3", "\uD83C\uDFE5", "\uD83C\uDFE6", "\uD83C\uDFEA", "\uD83C\uDFE9", "\uD83C\uDFE8", "\uD83D\uDC92", "\u26EA",
			"\uD83C\uDFEC", "\uD83C\uDF07", "\uD83C\uDF06", "\uD83C\uDFE7", "\uD83C\uDFEF", "\uD83C\uDFF0", "\u26FA", "\uD83C\uDFED", "\uD83D\uDDFC", "\uD83D\uDDFB",
			"\uD83C\uDF04", "\uD83C\uDF05", "\uD83C\uDF03", "\uD83D\uDDFD", "\uD83C\uDF08", "\uD83C\uDFA1", "\u26F2", "\uD83C\uDFA2", "\uD83D\uDEA2", "\uD83D\uDEA4", "\u26F5",
			"\u2708", "\uD83D\uDE80", "\uD83D\uDEB2", "\uD83D\uDE99", "\uD83D\uDE97", "\uD83D\uDE95", "\uD83D\uDE8C", "\uD83D\uDE93", "\uD83D\uDE92", "\uD83D\uDE91",
			"\uD83D\uDE9A", "\uD83D\uDE83", "\uD83D\uDE89", "\uD83D\uDE84", "\uD83D\uDE85", "\uD83C\uDFAB", "\u26FD", "\uD83D\uDEA5", "\u26A0", "\uD83D\uDEA7", "\uD83D\uDD30",
			"\uD83C\uDFB0", "\uD83D\uDE8F", "\uD83D\uDC88", "\u2668", "\uD83C\uDFC1", "\uD83C\uDF8C", "\uD83C\uDDEF\uD83C\uDDF5", "\uD83C\uDDF0\uD83C\uDDF7",
			"\uD83C\uDDE8\uD83C\uDDF3", "\uD83C\uDDFA\uD83C\uDDF8", "\uD83C\uDDEB\uD83C\uDDF7", "\uD83C\uDDEA\uD83C\uDDF8", "\uD83C\uDDEE\uD83C\uDDF9", "\uD83C\uDDF7\uD83C\uDDFA",
			"\uD83C\uDDEC\uD83C\uDDE7", "\uD83C\uDDE9\uD83C\uDDEA", "\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3", "\u0035\u20E3", "\u0036\u20E3", "\u0037\u20E3",
			"\u0038\u20E3", "\u0039\u20E3", "\u0030\u20E3", "\u0023\u20E3", "\u2B06", "\u2B07", "\u2B05", "\u27A1", "\u2197", "\u2196", "\u2198", "\u2199", "\u25C0", "\u25B6",
			"\u23EA", "\u23E9", "\uD83C\uDD97", "\uD83C\uDD95", "\uD83D\uDD1D", "\uD83C\uDD99", "\uD83C\uDD92", "\uD83C\uDFA6", "\uD83C\uDE01", "\uD83D\uDCF6", "\uD83C\uDE35",
			"\uD83C\uDE33", "\uD83C\uDE50", "\uD83C\uDE39", "\uD83C\uDE2F", "\uD83C\uDE3A", "\uD83C\uDE36", "\uD83C\uDE1A", "\uD83C\uDE37", "\uD83C\uDE38", "\uD83C\uDE02",
			"\uD83D\uDEBB", "\uD83D\uDEB9", "\uD83D\uDEBA", "\uD83D\uDEBC", "\uD83D\uDEAD", "\uD83C\uDD7F", "\u267F", "\uD83D\uDE87", "\uD83D\uDEBE", "\u3299", "\u3297",
			"\uD83D\uDD1E", "\uD83C\uDD94", "\u2733", "\u2734", "\uD83D\uDC9F", "\uD83C\uDD9A", "\uD83D\uDCF3", "\uD83D\uDCF4", "\uD83D\uDCB9", "\uD83D\uDCB1", "\u2648", "\u2649",
			"\u264A", "\u264B", "\u264C", "\u264D", "\u264E", "\u264F", "\u2650", "\u2651", "\u2652", "\u2653", "\u26CE", "\uD83D\uDD2F", "\uD83C\uDD70", "\uD83C\uDD71",
			"\uD83C\uDD8E", "\uD83C\uDD7E", "\uD83D\uDD32", "\uD83D\uDD34", "\uD83D\uDD33", "\uD83D\uDD5B", "\uD83D\uDD50", "\uD83D\uDD51", "\uD83D\uDD52", "\uD83D\uDD53",
			"\uD83D\uDD54", "\uD83D\uDD55", "\uD83D\uDD56", "\uD83D\uDD57", "\uD83D\uDD58", "\uD83D\uDD59", "\uD83D\uDD5A", "\u2B55", "\u274C", "\u00A9", "\u00AE", "\u2122", };

	public static final int[] DEFAULT_SMILEY_RES_IDS = { R.drawable.emo_im_01_bigsmile, R.drawable.emo_im_02_happy, R.drawable.emo_im_03_laugh, R.drawable.emo_im_04_smile,
			R.drawable.emo_im_05_wink, R.drawable.emo_im_06_adore, R.drawable.emo_im_07_kiss, R.drawable.emo_im_08_kissed, R.drawable.emo_im_09_expressionless,
			R.drawable.emo_im_10_pudently, R.drawable.emo_im_11_satisfied, R.drawable.emo_im_12_giggle, R.drawable.emo_im_13_impish, R.drawable.emo_im_14_disappointment,
			R.drawable.emo_im_15_beuptonogood, R.drawable.emo_im_16_frustrated, R.drawable.emo_im_17_sad, R.drawable.emo_im_18_sorry, R.drawable.emo_im_19_cry,
			R.drawable.emo_im_20_boring, R.drawable.emo_im_21_hungry, R.drawable.emo_im_22_scared, R.drawable.emo_im_23_shock, R.drawable.emo_im_24_sweat,
			R.drawable.emo_im_25_crying, R.drawable.emo_im_26_lol, R.drawable.emo_im_27_woo, R.drawable.emo_im_28_surprise, R.drawable.emo_im_29_frown, R.drawable.emo_im_30_angry,
			R.drawable.emo_im_31_wornout, R.drawable.emo_im_32_stop, R.drawable.emo_im_33_furious, R.drawable.emo_im_34_smoking, R.drawable.emo_im_35_hysterical,
			R.drawable.emo_im_36_exclamation, R.drawable.emo_im_37_question, R.drawable.emo_im_38_sleep, R.drawable.emo_im_39_aggressive, R.drawable.emo_im_40_badly,
			R.drawable.emo_im_41_singing, R.drawable.emo_im_42_bomb, R.drawable.emo_im_43_beaten, R.drawable.emo_im_44_thumbsdown, R.drawable.emo_im_45_thumbsup,
			R.drawable.emo_im_46_beer, R.drawable.emo_im_47_call, R.drawable.emo_im_48_hi, R.drawable.emo_im_49_hug, R.drawable.emo_im_50_facepalm, R.drawable.emo_im_51_easymoney,
			R.drawable.emo_im_52_dizzy, R.drawable.emo_im_53_disgust, R.drawable.emo_im_54_cocktail, R.drawable.emo_im_55_coffee, R.drawable.emo_im_56_cold,
			R.drawable.emo_im_57_cool, R.drawable.emo_im_58_despair, R.drawable.emo_im_59_hypnotic, R.drawable.emo_im_60_stars, R.drawable.emo_im_61_idea,
			R.drawable.emo_im_62_monocle, R.drawable.emo_im_63_movie, R.drawable.emo_im_64_music, R.drawable.emo_im_65_nerd, R.drawable.emo_im_66_ninja,
			R.drawable.emo_im_67_party, R.drawable.emo_im_68_pirate, R.drawable.emo_im_69_rage, R.drawable.emo_im_70_rose, R.drawable.emo_im_71_sick, R.drawable.emo_im_72_snotty,
			R.drawable.emo_im_73_stressed, R.drawable.emo_im_74_struggle, R.drawable.emo_im_75_study, R.drawable.emo_im_76_sweetangel, R.drawable.emo_im_77_thinking,
			R.drawable.emo_im_78_waiting, R.drawable.emo_im_79_whistling, R.drawable.emo_im_80_yawn, R.drawable.emo_im_81_exciting, R.drawable.emo_im_82_big_smile,
			R.drawable.emo_im_83_haha, R.drawable.emo_im_84_victory, R.drawable.emo_im_85_red_heart, R.drawable.emo_im_86_amazing, R.drawable.emo_im_87_black_heart,
			R.drawable.emo_im_88_what, R.drawable.emo_im_89_bad_smile, R.drawable.emo_im_90_bad_egg, R.drawable.emo_im_91_grimace, R.drawable.emo_im_92_girl,
			R.drawable.emo_im_93_greedy, R.drawable.emo_im_94_anger, R.drawable.emo_im_95_eyes_droped, R.drawable.emo_im_96_happy, R.drawable.emo_im_97_horror,
			R.drawable.emo_im_98_money, R.drawable.emo_im_99_nothing, R.drawable.emo_im_100_nothing_to_say, R.drawable.emo_im_101_cry, R.drawable.emo_im_102_scorn,
			R.drawable.emo_im_103_secret_smile, R.drawable.emo_im_104_shame, R.drawable.emo_im_105_shocked, R.drawable.emo_im_106_super_man, R.drawable.emo_im_107_the_iron_man,
			R.drawable.emo_im_108_unhappy, R.drawable.emo_im_109_electric_shock, R.drawable.emo_im_110_beaten, R.drawable.emo_im_111_grin, R.drawable.emo_im_112_happy,
			R.drawable.emo_im_113_fake_smile, R.drawable.emo_im_114_in_love, R.drawable.emo_im_115_kiss, R.drawable.emo_im_116_straight_face, R.drawable.emo_im_117_meaw,
			R.drawable.emo_im_118_drunk, R.drawable.emo_im_119_x_x, R.drawable.emo_im_120_youre_kidding_right, R.drawable.emo_im_122_sweat, R.drawable.emo_im_123_nerd,
			R.drawable.emo_im_124_angry, R.drawable.emo_im_125_disappearing, R.drawable.emo_im_126_dizzy, R.drawable.emo_im_127_music, R.drawable.emo_im_128_evilish,
			R.drawable.emo_im_129_graffiti, R.drawable.emo_im_130_omg, R.drawable.emo_im_131_on_fire, R.drawable.emo_im_132_ouch, R.drawable.emo_im_133_angry,
			R.drawable.emo_im_134_serious_business, R.drawable.emo_im_135_sick, R.drawable.emo_im_136_slow, R.drawable.emo_im_137_snooty, R.drawable.emo_im_138_suspicious,
			R.drawable.emo_im_139_crying, R.drawable.emo_im_140_want, R.drawable.emo_im_141_we_all_gonna_die, R.drawable.emo_im_142_wut, R.drawable.emo_im_143_boo,
			R.drawable.emo_im_144_xd, R.drawable.emo_im_145_kaboom, R.drawable.emo_im_146_yarr, R.drawable.emo_im_147_ninja, R.drawable.emo_im_148_yuush,
			R.drawable.emo_im_149_brains, R.drawable.emo_im_150_sleeping, R.drawable.emo_im_151_auto, R.drawable.emo_im_152_batti, R.drawable.emo_im_153_best,
			R.drawable.emo_im_154_metro, R.drawable.emo_im_155_biscuit, R.drawable.emo_im_156_chai, R.drawable.emo_im_157_chips, R.drawable.emo_im_158_samosa,
			R.drawable.emo_im_159_noodles, R.drawable.emo_im_160_jalebi, R.drawable.emo_im_161_icecream, R.drawable.emo_im_162_nariyal, R.drawable.emo_im_163_rum,
			R.drawable.emo_im_164_paisa, R.drawable.emo_im_165_tiranga, R.drawable.emo_im_166_hero, R.drawable.emo_im_167_neta, R.drawable.emo_im_168_police,
			R.drawable.emo_im_169_nimbu, R.drawable.emo_im_170_patakha, R.drawable.emo_im_171_love, R.drawable.emo_im_172_cupid, R.drawable.emo_im_173_shaktiman,
			R.drawable.emo_im_174_tandoori, R.drawable.emo_im_175_dd, R.drawable.e415, R.drawable.e056, R.drawable.e057, R.drawable.e414, R.drawable.e405, R.drawable.e106,
			R.drawable.e418, R.drawable.e417, R.drawable.e40d, R.drawable.e40a, R.drawable.e404, R.drawable.e105, R.drawable.e409, R.drawable.e40e, R.drawable.e402,
			R.drawable.e108, R.drawable.e403, R.drawable.e058, R.drawable.e407, R.drawable.e401, R.drawable.e40f, R.drawable.e40b, R.drawable.e406, R.drawable.e413,
			R.drawable.e411, R.drawable.e412, R.drawable.e410, R.drawable.e107, R.drawable.e059, R.drawable.e416, R.drawable.e408, R.drawable.e40c, R.drawable.e11a,
			R.drawable.e10c, R.drawable.e32c, R.drawable.e32a, R.drawable.e32d, R.drawable.e328, R.drawable.e32b, R.drawable.e022, R.drawable.e023, R.drawable.e327,
			R.drawable.e329, R.drawable.e32e, R.drawable.e335, R.drawable.e334, R.drawable.e337, R.drawable.e336, R.drawable.e13c, R.drawable.e330, R.drawable.e331,
			R.drawable.e326, R.drawable.e03e, R.drawable.e11d, R.drawable.e05a, R.drawable.e00e, R.drawable.e421, R.drawable.e420, R.drawable.e00d, R.drawable.e010,
			R.drawable.e011, R.drawable.e41e, R.drawable.e012, R.drawable.e422, R.drawable.e22e, R.drawable.e22f, R.drawable.e231, R.drawable.e230, R.drawable.e427,
			R.drawable.e41d, R.drawable.e00f, R.drawable.e41f, R.drawable.e14c, R.drawable.e201, R.drawable.e115, R.drawable.e428, R.drawable.e51f, R.drawable.e429,
			R.drawable.e424, R.drawable.e423, R.drawable.e253, R.drawable.e426, R.drawable.e111, R.drawable.e425, R.drawable.e31e, R.drawable.e31f, R.drawable.e31d,
			R.drawable.e001, R.drawable.e002, R.drawable.e005, R.drawable.e004, R.drawable.e51a, R.drawable.e519, R.drawable.e518, R.drawable.e515, R.drawable.e516,
			R.drawable.e517, R.drawable.e51b, R.drawable.e152, R.drawable.e04e, R.drawable.e51c, R.drawable.e51e, R.drawable.e11c, R.drawable.e536, R.drawable.e003,
			R.drawable.e41c, R.drawable.e41b, R.drawable.e419, R.drawable.e41a, R.drawable.e04a, R.drawable.e04b, R.drawable.e049, R.drawable.e048, R.drawable.e04c,
			R.drawable.e13d, R.drawable.e443, R.drawable.e43e, R.drawable.e04f, R.drawable.e052, R.drawable.e053, R.drawable.e524, R.drawable.e52c, R.drawable.e52a,
			R.drawable.e531, R.drawable.e050, R.drawable.e527, R.drawable.e051, R.drawable.e10b, R.drawable.e52b, R.drawable.e52f, R.drawable.e109, R.drawable.e528,
			R.drawable.e01a, R.drawable.e134, R.drawable.e530, R.drawable.e529, R.drawable.e526, R.drawable.e52d, R.drawable.e521, R.drawable.e523, R.drawable.e52e,
			R.drawable.e055, R.drawable.e525, R.drawable.e10a, R.drawable.e522, R.drawable.e019, R.drawable.e054, R.drawable.e520, R.drawable.e306, R.drawable.e030,
			R.drawable.e304, R.drawable.e110, R.drawable.e032, R.drawable.e305, R.drawable.e303, R.drawable.e118, R.drawable.e447, R.drawable.e119, R.drawable.e307,
			R.drawable.e308, R.drawable.e444, R.drawable.e441, R.drawable.e436, R.drawable.e437, R.drawable.e438, R.drawable.e43a, R.drawable.e439, R.drawable.e43b,
			R.drawable.e117, R.drawable.e440, R.drawable.e442, R.drawable.e446, R.drawable.e445, R.drawable.e11b, R.drawable.e448, R.drawable.e033, R.drawable.e112,
			R.drawable.e325, R.drawable.e312, R.drawable.e310, R.drawable.e126, R.drawable.e127, R.drawable.e008, R.drawable.e03d, R.drawable.e00c, R.drawable.e12a,
			R.drawable.e00a, R.drawable.e00b, R.drawable.e009, R.drawable.e316, R.drawable.e129, R.drawable.e141, R.drawable.e142, R.drawable.e317, R.drawable.e128,
			R.drawable.e14b, R.drawable.e211, R.drawable.e114, R.drawable.e145, R.drawable.e144, R.drawable.e03f, R.drawable.e313, R.drawable.e116, R.drawable.e10f,
			R.drawable.e104, R.drawable.e103, R.drawable.e101, R.drawable.e102, R.drawable.e13f, R.drawable.e140, R.drawable.e11f, R.drawable.e12f, R.drawable.e031,
			R.drawable.e30e, R.drawable.e311, R.drawable.e113, R.drawable.e30f, R.drawable.e13b, R.drawable.e42b, R.drawable.e42a, R.drawable.e018, R.drawable.e016,
			R.drawable.e015, R.drawable.e014, R.drawable.e42c, R.drawable.e42d, R.drawable.e017, R.drawable.e013, R.drawable.e20e, R.drawable.e20c, R.drawable.e20f,
			R.drawable.e20d, R.drawable.e131, R.drawable.e12b, R.drawable.e130, R.drawable.e12d, R.drawable.e324, R.drawable.e301, R.drawable.e148, R.drawable.e502,
			R.drawable.e03c, R.drawable.e30a, R.drawable.e042, R.drawable.e040, R.drawable.e041, R.drawable.e12c, R.drawable.e007, R.drawable.e31a, R.drawable.e13e,
			R.drawable.e31b, R.drawable.e006, R.drawable.e302, R.drawable.e319, R.drawable.e321, R.drawable.e322, R.drawable.e314, R.drawable.e503, R.drawable.e10e,
			R.drawable.e318, R.drawable.e43c, R.drawable.e11e, R.drawable.e323, R.drawable.e31c, R.drawable.e034, R.drawable.e035, R.drawable.e045, R.drawable.e338,
			R.drawable.e047, R.drawable.e30c, R.drawable.e044, R.drawable.e30b, R.drawable.e043, R.drawable.e120, R.drawable.e33b, R.drawable.e33f, R.drawable.e341,
			R.drawable.e34c, R.drawable.e344, R.drawable.e342, R.drawable.e33d, R.drawable.e33e, R.drawable.e340, R.drawable.e34d, R.drawable.e339, R.drawable.e147,
			R.drawable.e343, R.drawable.e33c, R.drawable.e33a, R.drawable.e43f, R.drawable.e34b, R.drawable.e046, R.drawable.e345, R.drawable.e346, R.drawable.e348,
			R.drawable.e347, R.drawable.e34a, R.drawable.e349, R.drawable.e036, R.drawable.e157, R.drawable.e038, R.drawable.e153, R.drawable.e155, R.drawable.e14d,
			R.drawable.e156, R.drawable.e501, R.drawable.e158, R.drawable.e43d, R.drawable.e037, R.drawable.e504, R.drawable.e44a, R.drawable.e146, R.drawable.e154,
			R.drawable.e505, R.drawable.e506, R.drawable.e122, R.drawable.e508, R.drawable.e509, R.drawable.e03b, R.drawable.e04d, R.drawable.e449, R.drawable.e44b,
			R.drawable.e51d, R.drawable.e44c, R.drawable.e124, R.drawable.e121, R.drawable.e433, R.drawable.e202, R.drawable.e135, R.drawable.e01c, R.drawable.e01d,
			R.drawable.e10d, R.drawable.e136, R.drawable.e42e, R.drawable.e01b, R.drawable.e15a, R.drawable.e159, R.drawable.e432, R.drawable.e430, R.drawable.e431,
			R.drawable.e42f, R.drawable.e01e, R.drawable.e039, R.drawable.e435, R.drawable.e01f, R.drawable.e125, R.drawable.e03a, R.drawable.e14e, R.drawable.e252,
			R.drawable.e137, R.drawable.e209, R.drawable.e133, R.drawable.e150, R.drawable.e320, R.drawable.e123, R.drawable.e132, R.drawable.e143, R.drawable.e50b,
			R.drawable.e514, R.drawable.e513, R.drawable.e50c, R.drawable.e50d, R.drawable.e511, R.drawable.e50f, R.drawable.e512, R.drawable.e510, R.drawable.e50e,
			R.drawable.e21c, R.drawable.e21d, R.drawable.e21e, R.drawable.e21f, R.drawable.e220, R.drawable.e221, R.drawable.e222, R.drawable.e223, R.drawable.e224,
			R.drawable.e225, R.drawable.e210, R.drawable.e232, R.drawable.e233, R.drawable.e235, R.drawable.e234, R.drawable.e236, R.drawable.e237, R.drawable.e238,
			R.drawable.e239, R.drawable.e23b, R.drawable.e23a, R.drawable.e23d, R.drawable.e23c, R.drawable.e24d, R.drawable.e212, R.drawable.e24c, R.drawable.e213,
			R.drawable.e214, R.drawable.e507, R.drawable.e203, R.drawable.e20b, R.drawable.e22a, R.drawable.e22b, R.drawable.e226, R.drawable.e227, R.drawable.e22c,
			R.drawable.e22d, R.drawable.e215, R.drawable.e216, R.drawable.e217, R.drawable.e218, R.drawable.e228, R.drawable.e151, R.drawable.e138, R.drawable.e139,
			R.drawable.e13a, R.drawable.e208, R.drawable.e14f, R.drawable.e20a, R.drawable.e434, R.drawable.e309, R.drawable.e315, R.drawable.e30d, R.drawable.e207,
			R.drawable.e229, R.drawable.e206, R.drawable.e205, R.drawable.e204, R.drawable.e12e, R.drawable.e250, R.drawable.e251, R.drawable.e14a, R.drawable.e149,
			R.drawable.e23f, R.drawable.e240, R.drawable.e241, R.drawable.e242, R.drawable.e243, R.drawable.e244, R.drawable.e245, R.drawable.e246, R.drawable.e247,
			R.drawable.e248, R.drawable.e249, R.drawable.e24a, R.drawable.e24b, R.drawable.e23e, R.drawable.e532, R.drawable.e533, R.drawable.e534, R.drawable.e535,
			R.drawable.e21a, R.drawable.e219, R.drawable.e21b, R.drawable.e02f, R.drawable.e024, R.drawable.e025, R.drawable.e026, R.drawable.e027, R.drawable.e028,
			R.drawable.e029, R.drawable.e02a, R.drawable.e02b, R.drawable.e02c, R.drawable.e02d, R.drawable.e02e, R.drawable.e332, R.drawable.e333, R.drawable.e24e,
			R.drawable.e24f, R.drawable.e537 };

	public static final int[] EMOJI_RES_IDS = { R.drawable.e415, R.drawable.e056, R.drawable.e057, R.drawable.e414, R.drawable.e405, R.drawable.e106, R.drawable.e418,
			R.drawable.e417, R.drawable.e40d, R.drawable.e40a, R.drawable.e404, R.drawable.e105, R.drawable.e409, R.drawable.e40e, R.drawable.e402, R.drawable.e108,
			R.drawable.e403, R.drawable.e058, R.drawable.e407, R.drawable.e401, R.drawable.e40f, R.drawable.e40b, R.drawable.e406, R.drawable.e413, R.drawable.e411,
			R.drawable.e412, R.drawable.e410, R.drawable.e107, R.drawable.e059, R.drawable.e416, R.drawable.e408, R.drawable.e40c, R.drawable.e11a, R.drawable.e10c,
			R.drawable.e32c, R.drawable.e32a, R.drawable.e32d, R.drawable.e328, R.drawable.e32b, R.drawable.e022, R.drawable.e023, R.drawable.e327, R.drawable.e329,
			R.drawable.e32e, R.drawable.e335, R.drawable.e334, R.drawable.e337, R.drawable.e336, R.drawable.e13c, R.drawable.e330, R.drawable.e331, R.drawable.e326,
			R.drawable.e03e, R.drawable.e11d, R.drawable.e05a, R.drawable.e00e, R.drawable.e421, R.drawable.e420, R.drawable.e00d, R.drawable.e010, R.drawable.e011,
			R.drawable.e41e, R.drawable.e012, R.drawable.e422, R.drawable.e22e, R.drawable.e22f, R.drawable.e231, R.drawable.e230, R.drawable.e427, R.drawable.e41d,
			R.drawable.e00f, R.drawable.e41f, R.drawable.e14c, R.drawable.e201, R.drawable.e115, R.drawable.e428, R.drawable.e51f, R.drawable.e429, R.drawable.e424,
			R.drawable.e423, R.drawable.e253, R.drawable.e426, R.drawable.e111, R.drawable.e425, R.drawable.e31e, R.drawable.e31f, R.drawable.e31d, R.drawable.e001,
			R.drawable.e002, R.drawable.e005, R.drawable.e004, R.drawable.e51a, R.drawable.e519, R.drawable.e518, R.drawable.e515, R.drawable.e516, R.drawable.e517,
			R.drawable.e51b, R.drawable.e152, R.drawable.e04e, R.drawable.e51c, R.drawable.e51e, R.drawable.e11c, R.drawable.e536, R.drawable.e003, R.drawable.e41c,
			R.drawable.e41b, R.drawable.e419, R.drawable.e41a, R.drawable.e04a, R.drawable.e04b, R.drawable.e049, R.drawable.e048, R.drawable.e04c, R.drawable.e13d,
			R.drawable.e443, R.drawable.e43e, R.drawable.e04f, R.drawable.e052, R.drawable.e053, R.drawable.e524, R.drawable.e52c, R.drawable.e52a, R.drawable.e531,
			R.drawable.e050, R.drawable.e527, R.drawable.e051, R.drawable.e10b, R.drawable.e52b, R.drawable.e52f, R.drawable.e109, R.drawable.e528, R.drawable.e01a,
			R.drawable.e134, R.drawable.e530, R.drawable.e529, R.drawable.e526, R.drawable.e52d, R.drawable.e521, R.drawable.e523, R.drawable.e52e, R.drawable.e055,
			R.drawable.e525, R.drawable.e10a, R.drawable.e522, R.drawable.e019, R.drawable.e054, R.drawable.e520, R.drawable.e306, R.drawable.e030, R.drawable.e304,
			R.drawable.e110, R.drawable.e032, R.drawable.e305, R.drawable.e303, R.drawable.e118, R.drawable.e447, R.drawable.e119, R.drawable.e307, R.drawable.e308,
			R.drawable.e444, R.drawable.e441, R.drawable.e436, R.drawable.e437, R.drawable.e438, R.drawable.e43a, R.drawable.e439, R.drawable.e43b, R.drawable.e117,
			R.drawable.e440, R.drawable.e442, R.drawable.e446, R.drawable.e445, R.drawable.e11b, R.drawable.e448, R.drawable.e033, R.drawable.e112, R.drawable.e325,
			R.drawable.e312, R.drawable.e310, R.drawable.e126, R.drawable.e127, R.drawable.e008, R.drawable.e03d, R.drawable.e00c, R.drawable.e12a, R.drawable.e00a,
			R.drawable.e00b, R.drawable.e009, R.drawable.e316, R.drawable.e129, R.drawable.e141, R.drawable.e142, R.drawable.e317, R.drawable.e128, R.drawable.e14b,
			R.drawable.e211, R.drawable.e114, R.drawable.e145, R.drawable.e144, R.drawable.e03f, R.drawable.e313, R.drawable.e116, R.drawable.e10f, R.drawable.e104,
			R.drawable.e103, R.drawable.e101, R.drawable.e102, R.drawable.e13f, R.drawable.e140, R.drawable.e11f, R.drawable.e12f, R.drawable.e031, R.drawable.e30e,
			R.drawable.e311, R.drawable.e113, R.drawable.e30f, R.drawable.e13b, R.drawable.e42b, R.drawable.e42a, R.drawable.e018, R.drawable.e016, R.drawable.e015,
			R.drawable.e014, R.drawable.e42c, R.drawable.e42d, R.drawable.e017, R.drawable.e013, R.drawable.e20e, R.drawable.e20c, R.drawable.e20f, R.drawable.e20d,
			R.drawable.e131, R.drawable.e12b, R.drawable.e130, R.drawable.e12d, R.drawable.e324, R.drawable.e301, R.drawable.e148, R.drawable.e502, R.drawable.e03c,
			R.drawable.e30a, R.drawable.e042, R.drawable.e040, R.drawable.e041, R.drawable.e12c, R.drawable.e007, R.drawable.e31a, R.drawable.e13e, R.drawable.e31b,
			R.drawable.e006, R.drawable.e302, R.drawable.e319, R.drawable.e321, R.drawable.e322, R.drawable.e314, R.drawable.e503, R.drawable.e10e, R.drawable.e318,
			R.drawable.e43c, R.drawable.e11e, R.drawable.e323, R.drawable.e31c, R.drawable.e034, R.drawable.e035, R.drawable.e045, R.drawable.e338, R.drawable.e047,
			R.drawable.e30c, R.drawable.e044, R.drawable.e30b, R.drawable.e043, R.drawable.e120, R.drawable.e33b, R.drawable.e33f, R.drawable.e341, R.drawable.e34c,
			R.drawable.e344, R.drawable.e342, R.drawable.e33d, R.drawable.e33e, R.drawable.e340, R.drawable.e34d, R.drawable.e339, R.drawable.e147, R.drawable.e343,
			R.drawable.e33c, R.drawable.e33a, R.drawable.e43f, R.drawable.e34b, R.drawable.e046, R.drawable.e345, R.drawable.e346, R.drawable.e348, R.drawable.e347,
			R.drawable.e34a, R.drawable.e349, R.drawable.e036, R.drawable.e157, R.drawable.e038, R.drawable.e153, R.drawable.e155, R.drawable.e14d, R.drawable.e156,
			R.drawable.e501, R.drawable.e158, R.drawable.e43d, R.drawable.e037, R.drawable.e504, R.drawable.e44a, R.drawable.e146, R.drawable.e154, R.drawable.e505,
			R.drawable.e506, R.drawable.e122, R.drawable.e508, R.drawable.e509, R.drawable.e03b, R.drawable.e04d, R.drawable.e449, R.drawable.e44b, R.drawable.e51d,
			R.drawable.e44c, R.drawable.e124, R.drawable.e121, R.drawable.e433, R.drawable.e202, R.drawable.e135, R.drawable.e01c, R.drawable.e01d, R.drawable.e10d,
			R.drawable.e136, R.drawable.e42e, R.drawable.e01b, R.drawable.e15a, R.drawable.e159, R.drawable.e432, R.drawable.e430, R.drawable.e431, R.drawable.e42f,
			R.drawable.e01e, R.drawable.e039, R.drawable.e435, R.drawable.e01f, R.drawable.e125, R.drawable.e03a, R.drawable.e14e, R.drawable.e252, R.drawable.e137,
			R.drawable.e209, R.drawable.e133, R.drawable.e150, R.drawable.e320, R.drawable.e123, R.drawable.e132, R.drawable.e143, R.drawable.e50b, R.drawable.e514,
			R.drawable.e513, R.drawable.e50c, R.drawable.e50d, R.drawable.e511, R.drawable.e50f, R.drawable.e512, R.drawable.e510, R.drawable.e50e, R.drawable.e21c,
			R.drawable.e21d, R.drawable.e21e, R.drawable.e21f, R.drawable.e220, R.drawable.e221, R.drawable.e222, R.drawable.e223, R.drawable.e224, R.drawable.e225,
			R.drawable.e210, R.drawable.e232, R.drawable.e233, R.drawable.e235, R.drawable.e234, R.drawable.e236, R.drawable.e237, R.drawable.e238, R.drawable.e239,
			R.drawable.e23b, R.drawable.e23a, R.drawable.e23d, R.drawable.e23c, R.drawable.e24d, R.drawable.e212, R.drawable.e24c, R.drawable.e213, R.drawable.e214,
			R.drawable.e507, R.drawable.e203, R.drawable.e20b, R.drawable.e22a, R.drawable.e22b, R.drawable.e226, R.drawable.e227, R.drawable.e22c, R.drawable.e22d,
			R.drawable.e215, R.drawable.e216, R.drawable.e217, R.drawable.e218, R.drawable.e228, R.drawable.e151, R.drawable.e138, R.drawable.e139, R.drawable.e13a,
			R.drawable.e208, R.drawable.e14f, R.drawable.e20a, R.drawable.e434, R.drawable.e309, R.drawable.e315, R.drawable.e30d, R.drawable.e207, R.drawable.e229,
			R.drawable.e206, R.drawable.e205, R.drawable.e204, R.drawable.e12e, R.drawable.e250, R.drawable.e251, R.drawable.e14a, R.drawable.e149, R.drawable.e23f,
			R.drawable.e240, R.drawable.e241, R.drawable.e242, R.drawable.e243, R.drawable.e244, R.drawable.e245, R.drawable.e246, R.drawable.e247, R.drawable.e248,
			R.drawable.e249, R.drawable.e24a, R.drawable.e24b, R.drawable.e23e, R.drawable.e532, R.drawable.e533, R.drawable.e534, R.drawable.e535, R.drawable.e21a,
			R.drawable.e219, R.drawable.e21b, R.drawable.e02f, R.drawable.e024, R.drawable.e025, R.drawable.e026, R.drawable.e027, R.drawable.e028, R.drawable.e029,
			R.drawable.e02a, R.drawable.e02b, R.drawable.e02c, R.drawable.e02d, R.drawable.e02e, R.drawable.e332, R.drawable.e333, R.drawable.e24e, R.drawable.e24f,
			R.drawable.e537 };

	public static final Map<Integer, Integer> moodMapping = new HashMap<Integer, Integer>();

	static
	{
		moodMapping.put(0, R.drawable.mood_01_happy);
		moodMapping.put(1, R.drawable.mood_02_sad);
		moodMapping.put(2, R.drawable.mood_03_in_love);
		moodMapping.put(3, R.drawable.mood_04_surprised);
		moodMapping.put(4, R.drawable.mood_05_confused);
		moodMapping.put(5, R.drawable.mood_06_angry);
		moodMapping.put(6, R.drawable.mood_07_sleepy);
		moodMapping.put(7, R.drawable.mood_08_hungover);
		moodMapping.put(8, R.drawable.mood_09_chilling);
		moodMapping.put(9, R.drawable.mood_10_studying);
		moodMapping.put(10, R.drawable.mood_11_busy);
		moodMapping.put(11, R.drawable.mood_12_love);
		moodMapping.put(12, R.drawable.mood_13_middle_finger);
		moodMapping.put(13, R.drawable.mood_14_boozing);
		moodMapping.put(14, R.drawable.mood_15_movie);
		moodMapping.put(15, R.drawable.mood_16_caffeinated);
		moodMapping.put(16, R.drawable.mood_17_insomniac);
		moodMapping.put(17, R.drawable.mood_18_driving);
		moodMapping.put(18, R.drawable.mood_19_traffic);
		moodMapping.put(19, R.drawable.mood_20_late);
		moodMapping.put(20, R.drawable.mood_21_shopping);
		moodMapping.put(21, R.drawable.mood_22_gaming);
		moodMapping.put(22, R.drawable.mood_23_coding);
		moodMapping.put(23, R.drawable.mood_24_television);
		moodMapping.put(33, R.drawable.mood_34_music);
		moodMapping.put(34, R.drawable.mood_35_partying_hard);
		moodMapping.put(35, R.drawable.mood_36_singing);
		moodMapping.put(36, R.drawable.mood_37_eating);
		moodMapping.put(37, R.drawable.mood_38_working_out);
		moodMapping.put(38, R.drawable.mood_39_cooking);
		moodMapping.put(39, R.drawable.mood_40_beauty_saloon);
		moodMapping.put(40, R.drawable.mood_41_sick);
	}
}
