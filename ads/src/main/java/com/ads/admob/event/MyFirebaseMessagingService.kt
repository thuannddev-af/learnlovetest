package com.ads.admob.event

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService(){
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.containsKey("af-uinstall-tracking")) { // "uinstall" is not a typo
            return
        } else {
            // handleNotification(remoteMessage);
        }
    }
}