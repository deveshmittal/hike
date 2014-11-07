package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by shobhitmandloi on 30/10/14.
 */
public class CardController {


    public static void callToAction(JSONObject androidIntent, Context mContext) throws URISyntaxException {
        Intent intent = Intent.parseUri(androidIntent.optString(HikePlatformConstants.INTENT_URI), 0);
        mContext.startActivity(intent);

    }
}
