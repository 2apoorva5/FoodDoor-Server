package com.server.fooddoor;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.tapadoo.alerter.Alerter;
import com.tapadoo.alerter.OnShowAlertListener;

import java.util.Arrays;

import es.dmoral.toasty.Toasty;

public class LoginActivity extends AppCompatActivity {

    //firebase variables
    FirebaseAuth firebaseAuth;

    //extra variables
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //make translucent statusBar on kitkat devices
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        //make fully Android Transparent Status bar
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() != null){
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        } else if(firebaseAuth.getCurrentUser() == null){
            Authenticate();
        }
    }

    private void Authenticate(){

        AuthMethodPickerLayout methodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.login_method_picker)
                .setEmailButtonId(R.id.email_login)
                .setGoogleButtonId(R.id.google_login)
                .build();

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build()))
                        .setIsSmartLockEnabled(false)
                        .setAuthMethodPickerLayout(methodPickerLayout)
                        .setTheme(R.style.ForLogin)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if(resultCode == RESULT_OK){
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            } else {
                if(response == null){
                    onBackPressed();
                    return;
                }

                if(response.getError().getErrorCode() == ErrorCodes.NO_NETWORK){
                    if (Build.VERSION.SDK_INT >= 21) {
                        Alerter.create(LoginActivity.this)
                                .setTitle("NO INTERNET CONNECTION!!!")
                                .setText("Make sure your Wi-Fi or Cellular Data is turned 'ON', then try again!")
                                .setTextAppearance(R.style.Alerter1TextAppearance)
                                .setBackgroundColorRes(R.color.Alerter1)
                                .setIcon(R.drawable.ic_error_outline_white_48dp)
                                .setDuration(5000)
                                .enableSwipeToDismiss()
                                .disableOutsideTouch()
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .setOnShowListener(new OnShowAlertListener() {
                                    @Override
                                    public void onShow() {
                                        setContentView(R.layout.no_internet_connection);
                                    }
                                })
                                .show();
                    }
                    return;
                }

                if(response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR){
                    Toasty.error(LoginActivity.this, "Unknown Error", Toast.LENGTH_SHORT, true).show();
                    return;
                }
            }
        }
    }

    public static void setWindowFlag(LoginActivity loginActivity, final int bits, boolean on) {
        Window window = loginActivity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        if(on){
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
        }
        window.setAttributes(layoutParams);
    }

    @Override
    public void onBackPressed() {
        LoginActivity.super.onBackPressed();
        LoginActivity.this.finish();
    }
}
