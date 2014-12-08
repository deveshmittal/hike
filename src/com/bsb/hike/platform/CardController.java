package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import com.bsb.hike.utils.Utils;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by shobhitmandloi on 30/10/14.
 */
public class CardController {


    public static void callToAction(JSONObject androidIntent, Context mContext) throws URISyntaxException {
        Intent intent = Intent.parseUri(androidIntent.optString(HikePlatformConstants.INTENT_URI), 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

    }

    public static void callToActionWebView(JSONObject androidIntent, Context mContext) throws URISyntaxException {
//        Intent intent = new Intent(mContext, WebViewActivity.class);
//        intent.putExtra("url", androidIntent.optString(HikePlatformConstants.INTENT_URI));
//        mContext.startActivity(intent);
        Utils.startWebViewActivity(mContext,androidIntent.optString(HikePlatformConstants.INTENT_URI), "Hike");

    }

}
