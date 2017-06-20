package com.github.guwenk.smuradio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

public class SignInDialog extends DialogFragment {
    private static final int RC_SIGN_IN = 9001;
    private static final int PICK_MUSIC_REQUEST = 9002;
    //AUTH
    private final String AuthTag = "Auth debug: ";
    AlertDialog alert;
    Button alertDialogButton;
    Button selectFileButton;
    CheckBox checkBox;
    boolean check1; // SIGN IN
    boolean check2; // LICENSE
    boolean check3; // FILE
    private String songTitle;
    private Uri filepath;
    private SignInButton signInButton;
    private AlertDialog.Builder builder;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    // /AUTH

    // STORAGE
    private StorageReference mStorageRef;
    // /STORAGE

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        builder = new AlertDialog.Builder(getActivity());
        View signInDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_sign_in, null);
        builder.setView(signInDialogView);
        builder.setMessage(R.string.upload_your_song);

        checkBox = (CheckBox) signInDialogView.findViewById(R.id.checkBox2);


        check1 = false;
        check2 = false;
        check3 = false;
        // Log.d(AuthTag, "onCreateDialog " + check1 + " " + check2 + " " + check3);

        this.mGoogleApiClient = OrderActivity.getmGoogleApiClient();
        mAuth = FirebaseAuth.getInstance();

        selectFileButton = (Button) signInDialogView.findViewById(R.id.selectFileButton);
        selectFileButton.setOnClickListener(new customButtonClickListener());


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
        ss.setSpan(clickableSpan, 14, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

        mStorageRef = FirebaseStorage.getInstance().getReference();

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
                firebaseAuthWithGoogle(account);
            } else {
                Log.d(AuthTag, "Result is not success");
            }
        }

        if (requestCode == PICK_MUSIC_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filepath = data.getData();
            songTitle = getFileName(filepath);
            Log.d(AuthTag, "SONG: " + songTitle);
            selectFileButton.setText(songTitle);
            selectFileButton.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_check_circle_24dp, null), null, null, null);
            check3 = true;
            buttonStatus();
        }
    }


    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    @Override
    public void onStart() {
        super.onStart();
        alertDialogButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        alertDialogButton.setEnabled(false);
        alertDialogButton.setOnClickListener(new customButtonClickListener());
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            signInButton.setEnabled(false);
            check1 = true;
        }

        Log.d(AuthTag, "onStart " + check1 + " " + check2 + " " + check3);
        buttonStatus();
    }

    private void buttonStatus() {
        if (check1 && check2 && check3) {
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
                            //Log.d(AuthTag, "onComplete " + check1 + " " + check2 + " " + check3);
                            buttonStatus();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(getActivity(), R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_music)), PICK_MUSIC_REQUEST);
    }

    private void uploadFile() {

        if (filepath != null) {
            Log.d(AuthTag, "UPLOAD FILE " + filepath);

            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.uploading));
            progressDialog.setCancelable(false);
            progressDialog.show();

            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            else {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }

            Log.d(AuthTag, "UPLOAD FILE progress dialog showing");

            StorageReference musicRef = mStorageRef.child("audio/" + songTitle);

            Log.d(AuthTag, "UPLOAD FILE storage referense: " + musicRef);

            musicRef.putFile(filepath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // if upload success
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), R.string.file_uploaded, Toast.LENGTH_SHORT).show();
                            alert.dismiss();
                            Log.d(AuthTag, "UPLOAD FILE success");
                            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // if upload failed
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), getString(R.string.uploading_error) + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            alert.dismiss();
                            Log.d(AuthTag, "UPLOAD FILE FAILED");
                            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            try {
                                progressDialog.setMessage((int) progress + getString(R.string.uploaded_procents));
                                Log.d(AuthTag, "UPLOAD FILE progress update: " + progress);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } else {
            Toast.makeText(getActivity(), R.string.wrong_file, Toast.LENGTH_SHORT).show();
        }
    }

    private class customButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == selectFileButton) {
                // open file chooser
                showFileChooser();
            } else if (v == alertDialogButton) {
                // upload file on web
                Log.d(AuthTag, "UPLOAD FILE");
                uploadFile();
            }
        }
    }

}
