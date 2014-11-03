package com.bsb.hike.platform;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;

import java.util.HashMap;
import java.util.List;

/**
 * Created by shobhit on 29/10/14.
 */
public class CardRenderer {

    int cardCount;
    Context mContext;

    public CardRenderer(int cardCount, Context context){
        this.cardCount = cardCount;
        this.mContext = context;

    }


    private static class ViewHolder
    {
        ViewStub dayStub;
        View dayStubInflated;
        ImageView status;
        TextView time;
        View timeStatus;
        View senderDetails;
        TextView senderName;
        TextView senderNameUnsaved;
        ImageView avatarImage;
        ViewGroup avatarContainer;
        View selectedStateOverlay;
        ViewGroup messageContainer;
        ViewStub messageInfoStub;
        View messageInfoInflated;
        HashMap<String, View> viewHashMap;

        public void initializeHolder( View view, List<CardComponent.TextComponent> textComponentList, List<CardComponent.MediaComponent> mediaComponentList) {

            viewHashMap = new HashMap<String, View>();
            time = (TextView) view.findViewById(R.id.time);
            status = (ImageView) view.findViewById(R.id.status);
            timeStatus = (View) view.findViewById(R.id.time_status);
            selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
            messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
            dayStub = (ViewStub) view.findViewById(R.id.day_stub);

            for (CardComponent.TextComponent textComponent : textComponentList) {
                String tag = textComponent.getTag();
                viewHashMap.put(tag, view.findViewWithTag(tag));
            }

            for (CardComponent.MediaComponent mediaComponent : mediaComponentList) {
                String tag = mediaComponent.getTag();
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
        return cardCount;
    }



    public View getView(View view, ConvMessage convMessage){

        int type = convMessage.platformMessageMetadata.layoutId;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        List<CardComponent.TextComponent> textComponents = convMessage.platformMessageMetadata.textComponents;
        List<CardComponent.MediaComponent> mediaComponents = convMessage.platformMessageMetadata.mediaComponents;

        if (type == CardConstants.IMAGE_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardDataFiller(textComponents, mediaComponents, viewHolder);

        }
        else if (type == CardConstants.VIDEO_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardDataFiller(textComponents, mediaComponents, viewHolder);

        }
        else if (type == CardConstants.GAMES_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();

                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardDataFiller(textComponents, mediaComponents, viewHolder);


        }
        else if (type == CardConstants.ARTICLE_CARD_LAYOUT)
        {
            ViewHolder viewHolder;
            if (view == null){
                viewHolder = new ViewHolder();
                if (convMessage.isSent()) {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForSender(view);
                }else {
                    view = inflater.inflate(R.layout.card_layout, null);
                    viewHolder.initializeHolderForReceiver(view);
                }
                viewHolder.initializeHolder(view, textComponents, mediaComponents);

                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            cardDataFiller(textComponents, mediaComponents, viewHolder);

        }

        return view;
    }


    private void cardDataFiller(List<CardComponent.TextComponent> textComponents, List<CardComponent.MediaComponent> mediaComponents, ViewHolder viewHolder) {
        for (CardComponent.TextComponent textComponent : textComponents) {
            String tag = textComponent.getTag();
            TextView tv = (TextView) viewHolder.viewHashMap.get(tag);
            tv.setText(textComponent.getText());
        }

        for (CardComponent.MediaComponent mediaComponent : mediaComponents) {
            String tag = mediaComponent.getTag();

            View mediaView =  viewHolder.viewHashMap.get(tag);
            if (mediaView instanceof ImageView)
                ((ImageView) mediaView).setImageBitmap(HikeBitmapFactory.stringToBitmap(mediaComponent.getBase64()));

        }
    }


}
