package com.github.guwenk.smuradio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import static android.content.ContentValues.TAG;

public class SignInDialog extends DialogFragment {
    private SignInButton signInButton;
    private AlertDialog.Builder builder;
    AlertDialog alert;
    Button alertDialogButton;
    CheckBox checkBox;
    //AUTH
    private final String AuthTag = "Auth debug: ";
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    // /AUTH

    boolean check1; // SIGN IN
    boolean check2; // LICENSE
    boolean check3; // FILE

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        builder = new AlertDialog.Builder(getActivity());
        View signInDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_sign_in, null);
        builder.setView(signInDialogView);
        builder.setMessage(R.string.upload_your_song);

        checkBox = (CheckBox) signInDialogView.findViewById(R.id.checkBox2);


        check1 = false; check2 = false; check3 = true;
        Log.d(AuthTag,"onCreateDialog " + check1 + " " + check2 + " " + check3);

        this.mGoogleApiClient = OrderActivity.getmGoogleApiClient();
        mAuth = FirebaseAuth.getInstance();

        signInButton = (SignInButton) signInDialogView.findViewById(R.id.sign_in_button);
        signInButton.setStyle(SignInButton.SIZE_WIDE, SignInButton.COLOR_AUTO);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        builder.setPositiveButton(getString(R.string.next), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO upload song
            }
        });

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        SpannableString ss = new SpannableString(getString(R.string.you_accepting_license_agreement));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Log.d(AuthTag, "click");
                LicenseDialog licenseDialog = new LicenseDialog();
                licenseDialog.show(getFragmentManager(), "Sing in dialog");
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        ss.setSpan(clickableSpan, 14, ss.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView textView = (TextView) signInDialogView.findViewById(R.id.textView);
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                check2 = isChecked;
                Log.d(AuthTag, "Checked: " + isChecked);
                buttonStatus();
            }
        });

        alert = builder.create();
        return alert;
    }




    private void signIn() {
        Log.d(AuthTag, "signIn");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                Log.d(AuthTag, "Result is success");
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                try {
                    Log.d(AuthTag, account.getEmail().toString());
                } catch (Exception e){}
                firebaseAuthWithGoogle(account);
            } else {
                Log.d(AuthTag, "Result is not success");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        alertDialogButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        alertDialogButton.setEnabled(false);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            signInButton.setEnabled(false);
            check1 = true;
        }

        Log.d(AuthTag, "onStart " + check1 + " " + check2 + " " + check3);
        buttonStatus();
    }

    private void buttonStatus(){
        if(check1 && check2 && check3) {
            alertDialogButton.setEnabled(true);
        } else {
            try {
                alertDialogButton.setEnabled(false); // ПЕРЕДЕЛАТЬ!
            } catch (Exception e) {
                Log.d(AuthTag, "Button status error");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            signInButton.setEnabled(false);
                            check1 = true;
                            Log.d(AuthTag,"onComplete " + check1 + " " + check2 + " " + check3);
                            buttonStatus();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(getActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

}
