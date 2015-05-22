package com.poterin.patra;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;

public class Speaker implements TextToSpeech.OnInitListener {

    private Context context;
    private TextToSpeech tts;
    private String errorMessage = null;
    private View currentStopView;

    public Speaker(final Context context) {
        this.context = context;
        tts = new TextToSpeech(context, this);
    } // Speaker

    @Override
    public void onInit(int status) {
        // todo Do it!
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            errorMessage = "This Android version will be supported soon!";
            return;
        }

        if (status != TextToSpeech.SUCCESS) {
            errorMessage = "Text to speech engine initialization failed!";
            return;
        }

        int result = tts.setLanguage(Languages.getLocaleById(Settings.primaryLanguage()));

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            errorMessage = "The language is not supported by text to speech engine!";
        }
    } // onInit

    public void speakOut(final String text, final View stopView) {
        if (errorMessage != null) {
            Toast toast = Toast.makeText(context, errorMessage, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (currentStopView != null) currentStopView.setVisibility(View.GONE);

        currentStopView = stopView;

        if (stopView != null) {
            stopView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    tts.stop();
                }
            });

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    stopView.post(new Runnable() {
                        @Override
                        public void run() {
                            stopView.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onDone(String s) {
                    stopView.post(new Runnable() {
                        @Override
                        public void run() {
                            stopView.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onError(String s) {
                    onDone(s);
                }
            }); // setOnUtteranceProgressListener
        }
        else
            tts.setOnUtteranceProgressListener(null);


        tts.setSpeechRate(Settings.speechSpeed());

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Speaker");

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    } // speakOut

    public void shutdown() {
        tts.stop();
        tts.shutdown();
    }
}
