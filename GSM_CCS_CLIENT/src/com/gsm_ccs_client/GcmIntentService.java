package com.gsm_ccs_client;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class GcmIntentService extends IntentService
{
     public GcmIntentService()
    {
	super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
	Bundle extras = intent.getExtras();
	GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
	// The getMessageType() intent parameter must be the intent you received
	// in your BroadcastReceiver.
	String messageType = gcm.getMessageType(intent);

	if (!extras.isEmpty()) // has effect of unparcelling Bundle
	{
	    /*
	     * Filter messages based on message type. Since it is likely that
	     * GCM will be extended in the future with new message types, just
	     * ignore any message types you're not interested in, or that you
	     * don't recognize.
	     */
	    if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType))
	    {
		    /*Log for now. It needs to be dealt with later*/
	    	Log.i(Globals.TAG, "SendError : " + messageType);
	    }
	    else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType))
	    {
			/*Log for now. It needs to be dealt with later*/
	    	Log.i(Globals.TAG, "DeletedMessage : " + messageType);
	    }
	    else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType))
	    {
		    /*Actual message. It needs to be implemented.*/
	    }
	}
	// Release the wake lock provided by the WakefulBroadcastReceiver.
	GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}