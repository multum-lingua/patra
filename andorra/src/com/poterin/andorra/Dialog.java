package com.poterin.andorra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import java.util.Arrays;

public class Dialog  {

    public static void showMessage(Context context, CharSequence title, CharSequence message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alert.show();
        ((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void showException(Context context, Exception e) {
        e.printStackTrace();
        showMessage(context, e.toString(), Arrays.toString(e.getStackTrace()));
    }

    public static void showMessage(
        Context context, CharSequence title, String message, DialogInterface.OnClickListener onOKClick)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, onOKClick);
        builder.show();
    }

    public static void showOKCancel(
        Context context, CharSequence title, String message, DialogInterface.OnClickListener onOKClick)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, onOKClick);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    public static class ModalDialog extends AlertDialog {

        private Handler mHandler;
        public int result = 0;

        public ModalDialog(Context context) {
            super(context);
        }

        public int showModal() {

            class BreakLoopException extends RuntimeException {}

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //super.handleMessage(msg);
                    throw new BreakLoopException();
                }
            };

            show();

            try {
                mHandler.getLooper().loop();
                //Looper.getMainLooper().loop();
            }
            catch(BreakLoopException e) {}

            return result;
        } // showModal

        @Override
        protected void onStop() {
            super.onStop();

            mHandler.sendMessage(mHandler.obtainMessage());
        }

        public void setButton(int whichButton, int resId) {
            super.setButton(whichButton, this.getContext().getString(resId), defaultClickListener);
        }

        public DialogInterface.OnClickListener defaultClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                result = which;
                dismiss();
            }
        };

    } // ModalDialog

}
