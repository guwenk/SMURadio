package com.github.guwenk.smuradio;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

public class LicenseDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View signInDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_license, null);
        builder.setView(signInDialogView);
        builder.setTitle("LICENSE AGREEMENT");
        //builder.setMessage("license agreement");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        return builder.create();
    }
}
