package com.github.guwenk.smuradio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.SignInButton;

/**
 * Created by Stron on 20-Jun-17.
 */

public class SignInDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View signInDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_sign_in, null);
        builder.setView(signInDialogView);

        builder.setMessage(R.string.upload_your_song);

        SignInButton signInButton = (SignInButton) signInDialogView.findViewById(R.id.sign_in_button);
        signInButton.setStyle(SignInButton.SIZE_WIDE, SignInButton.COLOR_AUTO);

        builder.setPositiveButton(getString(R.string.next), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO upload song
            }
        });

        SpannableString ss = new SpannableString(getString(R.string.you_accepting_license_agreement));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Log.d("55555555", "click");
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

        return builder.create();
    }
}
