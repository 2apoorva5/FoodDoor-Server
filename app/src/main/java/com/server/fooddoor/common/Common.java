package com.server.fooddoor.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.server.fooddoor.model.OrderRequests;
import com.server.fooddoor.remote.APIService;
import com.server.fooddoor.remote.FCMRetrofitClient;
import com.server.fooddoor.remote.IGeoCoordinates;
import com.server.fooddoor.remote.RetrofitClient;

public class Common {

    public static String EMAIL_TEXT = "userEmail";

    public static OrderRequests currentRequests;

    public static final String baseUrl = "https://maps.googleapis.com";

    public static final String FCM_URL = "https://fcm.googleapis.com";

    public static IGeoCoordinates getGeoCodeService()
    {
        return RetrofitClient.getClient(baseUrl).create(IGeoCoordinates.class);
    }

    public static APIService getFCMClient()
    {
        return FCMRetrofitClient.getClient(FCM_URL).create(APIService.class);
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight)
    {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float scaleX = newWidth/(float)bitmap.getWidth();
        float scaleY = newHeight/(float)bitmap.getHeight();
        float pivotX = 0, pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }
}
