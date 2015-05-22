package com.poterin.patra;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.poterin.patra.UserDictionary.userDictionary;

public class WordsTraining {
    private final DictionaryActivity dictionaryActivity;
    private int currentWordIndex;
    private String currentWord;
    private List<Element> words = new ArrayList<>();
    private int totalWords = 0;
    private int knownWords = 0;

    public WordsTraining(DictionaryActivity dictionaryActivity) {
        this.dictionaryActivity = dictionaryActivity;

        for (int i = 0; i < userDictionary.items().getLength(); i++) {
            Element item = userDictionary.getItem(i);
            if (userDictionary.studyProgress(item) < 100) words.add(item);
        }

        if (words.size() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(dictionaryActivity);
            builder.setTitle(R.string.menu_my_dictionary);
            builder.setMessage(R.string.no_unstudied_words);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
            return;
        }

        dictionaryActivity.listView.setVisibility(View.GONE);

        showWordDialog();
    } // WordsTraining

    private void getNextWord() {
        currentWordIndex = (int) (Math.random() * words.size());
        Element item = words.get(currentWordIndex);
        if (userDictionary.directProgress(item) < 2) currentWord = item.getAttribute("phrase");
        else currentWord = item.getAttribute("translation");
    }

    private void showWordDialog() {
        getNextWord();

        AlertDialog.Builder builder = new AlertDialog.Builder(dictionaryActivity);
        builder.setMessage(currentWord);

        View dialogView = dictionaryActivity.getLayoutInflater().inflate(R.layout.words_training_dialog, null);
        builder.setCustomTitle(dialogView);

        builder.setPositiveButton(R.string.i_know, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showCheckDialog(false);
            }
        });

        builder.setNegativeButton(R.string.i_dont_know, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showCheckDialog(true);
            }
        });

        final AlertDialog dialog = builder.create();

        ImageButton imageButtonVoice = (ImageButton) dialogView.findViewById(R.id.imageButtonVoice);
        imageButtonVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dictionaryActivity.speaker.speakOut(currentWord, null);
            }
        });

        ImageButton imageButtonClose = (ImageButton) dialogView.findViewById(R.id.imageButtonClose);
        imageButtonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (totalWords > 0) showStatisticsDialog();
                else dictionaryActivity.refresh();
            }
        });

        dialog.show();

        if (Settings.voiceInTraining())
            dictionaryActivity.speaker.speakOut(currentWord, null);
    } // showWordDialog

    private void showCheckDialog(boolean ok) {
        totalWords++;
        final Element item = words.get(currentWordIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(dictionaryActivity);
        builder.setCancelable(false);

        View dialogView = dictionaryActivity.getLayoutInflater().inflate(R.layout.check_word_dialog, null);
        builder.setCustomTitle(dialogView);

        TextView textViewTitle = (TextView) dialogView.findViewById(R.id.textViewTitle);
        textViewTitle.setText(item.getAttribute("phrase"));

        ImageButton imageButtonVoice = (ImageButton) dialogView.findViewById(R.id.imageButtonVoice);
        imageButtonVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dictionaryActivity.speaker.speakOut(item.getAttribute("phrase"), null);
            }
        });

        builder.setMessage(item.getAttribute("translation"));

        if (ok)
            builder.setNeutralButton(android.R.string.ok, null);
        else {
            builder.setPositiveButton(R.string.i_knew, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    knownWords++;
                    if (userDictionary.directProgress(item) < 2)
                        item.setAttribute("direct_progress", String.valueOf(userDictionary.directProgress(item) + 1));
                    else
                        item.setAttribute("reverse_progress", String.valueOf(userDictionary.reverseProgress(item) + 1));
                }
            });

            builder.setNegativeButton(R.string.i_didnt_know, null);
        } // if

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                words.remove(currentWordIndex);
                if (words.size() > 0) showWordDialog();
                else {
                    showStatisticsDialog();
                }
            }
        });

        dialog.show();

        if (Settings.voiceInTraining() && userDictionary.directProgress(item) >= 2)
            dictionaryActivity.speaker.speakOut(item.getAttribute("phrase"), null);
    } // showCheckDialog

    private void showStatisticsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(dictionaryActivity);
        builder.setTitle(R.string.study_words);
        builder.setMessage(
            dictionaryActivity.getString(R.string.studying_statistics, totalWords, knownWords));
        builder.setNeutralButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dictionaryActivity.refresh();
                userDictionary.save();
            }
        });

        dialog.show();
    } // showStatisticsDialog
}
