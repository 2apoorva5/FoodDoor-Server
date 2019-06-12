package com.server.fooddoor.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.server.fooddoor.model.Token;

public class MyFirebaseIdService extends FirebaseInstanceIdService {

    FirebaseAuth auth;
    FirebaseUser user;

        @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if(user != null)
        {
            updateToServer(refreshedToken);
        }
        else
        {
            return;
        }
    }

    private void updateToServer(String refreshedToken) {
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference tokens = database.getReference("Tokens");
        Token token = new Token(refreshedToken, true);       //true because this is the server side
        tokens.child(user.getUid()).setValue(token);
    }
}
