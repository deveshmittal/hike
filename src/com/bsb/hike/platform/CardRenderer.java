package com.bsb.hike.platform;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.view.CustomFontTextView;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by shobhit on 29/10/14.
 */
public class CardRenderer implements View.OnLongClickListener {

    Context mContext;

    public CardRenderer(Context context){
        this.mContext = context;

    }

    private static final int IMAGE_CARD_LAYOUT_SENT = 0;
    private static final int IMAGE_CARD_LAYOUT_RECEIVED = 1;
    private static final int VIDEO_CARD_LAYOUT_SENT = 2;
    private static final int VIDEO_CARD_LAYOUT_RECEIVED = 3;
    private static final int GAMES_CARD_LAYOUT_SENT = 4;
    private static final int GAMES_CARD_LAYOUT_RECEIVED = 5;
    private static final int ARTICLE_CARD_LAYOUT_SENT = 6;
    private static final int ARTICLE_CARD_LAYOUT_RECEIVED = 7;




    public static class ViewHolder extends MessagesAdapter.DetailViewHolder
    {

        HashMap<String, View> viewHashMap;

        public void initializeHolder(View view, List<CardComponent.TextComponent> textComponentList, List<CardComponent.MediaComponent> mediaComponentList, ArrayList<CardComponent.ActionComponent> actionComponents) {

            viewHashMap = new HashMap<String, View>();
            time = (TextView) view.findViewById(R.id.time);
            status = (ImageView) view.findViewById(R.id.status);
            timeStatus = (View) view.findViewById(R.id.time_status);
            selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
            messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
            dayStub = (ViewStub) view.findViewById(R.id.day_stub);
            messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);


            for (CardComponent.TextComponent textComponent : textComponentList) {
                String tag = textComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.MediaComponent mediaComponent : mediaComponentList) {
                String tag = mediaComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.ActionComponent actionComponent : actionComponents) {
                String tag = actionComponent.getTag();
                if (!TextUtils.isEmpty(tag))
                    viewHashMap.put(tag, view.findViewWithTag(tag));
            }

        }

        public void initializeHolderForSender(View view){

            messageInfoStub = (ViewStub) view.findViewById(R.id.message_info_stub);

        }

        public void initializeHolderForReceiver(View view){

            senderDetails = view.findViewById(R.id.sender_details);
            senderName = (TextView) view.findViewById(R.id.sender_name);
            senderNameUnsaved = (TextView) view.findViewById(R.id.sender_unsaved_name);
            avatarImage = (ImageView) view.findViewById(R.id.avatar);
            avatarContainer = (ViewGroup) view.findViewById(R.id.avatar_container);

        }

    }


    public int getCardCount() {
        return CardConstants.CHAT_THREAD_CARD_COUNT_TYPE_COUNT;
    }

    public int getItemViewType(ConvMessage convMessage) {

        Log.d(CardRenderer.class.getSimpleName(), "hash code for convMessage is " + String.valueOf(convMessage.hashCode()));
        int cardType = convMessage.platformMessageMetadata.layoutId;
        if (convMessage.isSent()) {

            switch (cardType) {
                case 1:
                    return IMAGE_CARD_LAYOUT_SENT;

                case 2:
                    return VIDEO_CARD_LAYOUT_SENT;

                case 3:
                    return GAMES_CARD_LAYOUT_SENT;

                case 4:
                    return ARTICLE_CARD_LAYOUT_SENT;
            }

        }
        else
        {
            switch (cardType) {
                case 1:
                    return IMAGE_CARD_LAYOUT_RECEIVED;

                case 2:
                    return VIDEO_CARD_LAYOUT_RECEIVED;

                case 3:
                    return GAMES_CARD_LAYOUT_RECEIVED;

                case 4:
                    return ARTICLE_CARD_LAYOUT_RECEIVED;
            }

        }
        return 0;

    }

    public View getView(View view, ConvMessage convMessage){

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int cardType = convMessage.platformMessageMetadata.layoutId;
        List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.textComponents;
        List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.mediaComponents;
        ArrayList<CardComponent.ActionComponent> actionComponents = convMessage.platformMessageMetadata.actionComponents;

        if (cardType == CardConstants.IMAGE_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_games_sent, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout_games_received, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardCallToActions(actionComponents, viewHolder, true, "");
            cardDataFiller(textComponents, mediaComponents, viewHolder);

        }
        else if (cardType == CardConstants.VIDEO_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_video_sent, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout_video_received, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardCallToActions(actionComponents, viewHolder, true, "");
            cardDataFiller(textComponents, mediaComponents, viewHolder);

        }
        else if (cardType == CardConstants.GAMES_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();

                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_games_sent, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout_games_received, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);

                view.setTag(viewHolder);

            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardDataFiller(textComponents, mediaComponents, viewHolder);
            String channelSource = convMessage.platformMessageMetadata.channelSource;
            boolean isGamesAppInstalled = convMessage.platformMessageMetadata.isInstalled;
            if (!isGamesAppInstalled) {
                gameInstalledTextFiller(viewHolder);
            }
            cardCallToActions(actionComponents, viewHolder, isGamesAppInstalled, channelSource);


        }
        else if (cardType == CardConstants.ARTICLE_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout_article_sent, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout_article_received, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents, actionComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardCallToActions(actionComponents, viewHolder, true, "");
            cardDataFiller(textComponents, mediaComponents, viewHolder);

        }

        return view;
    }



    private void cardCallToActions(ArrayList<CardComponent.ActionComponent> actionComponents, ViewHolder viewHolder, final boolean isAppInstalled, final String channelSource) {
        for (final CardComponent.ActionComponent actionComponent : actionComponents) {
            final String tag = actionComponent.getTag();
            if (!TextUtils.isEmpty(tag)) {
                View actionView = viewHolder.viewHashMap.get(tag);
                actionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (tag.equalsIgnoreCase("CARD") && !isAppInstalled) {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put(HikePlatformConstants.INTENT_URI,"https://play.google.com/store/apps/details?id=" + channelSource);
                                CardController.callToAction(jsonObject, mContext);
                            } else {
                                CardController.callToAction(actionComponent.getAndroidIntent(), mContext);
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                actionView.setOnLongClickListener(this);

            }
        }
    }


    private void cardDataFiller(List<CardComponent.TextComponent> textComponents, List<CardComponent.MediaComponent> mediaComponents, ViewHolder viewHolder) {
        for (CardComponent.TextComponent textComponent : textComponents) {
            String tag = textComponent.getTag();
            if (!TextUtils.isEmpty(tag)) {

                CustomFontTextView tv = (CustomFontTextView) viewHolder.viewHashMap.get(tag);
                tv.setText(textComponent.getText());
            }
        }

        for (CardComponent.MediaComponent mediaComponent : mediaComponents) {
            String tag = mediaComponent.getTag();

            if (!TextUtils.isEmpty(tag)) {
                View mediaView = viewHolder.viewHashMap.get(tag);
                if (mediaView instanceof ImageView) {
                    String data = mediaComponent.getKey();
                    Drawable value = HikeMessengerApp.getLruCache().getBitmapDrawable(data);


                    ((ImageView) mediaView).setImageDrawable(value);

                }

            }
        }

    }

    private void gameInstalledTextFiller(ViewHolder viewHolder) {
        if (viewHolder.viewHashMap.containsKey("T4")) {
            CustomFontTextView cardInstalledText = (CustomFontTextView) viewHolder.viewHashMap.get("T4");
            cardInstalledText.setText("INSTALL");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }




}
