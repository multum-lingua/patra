package com.poterin.patra;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import com.poterin.andorra.Dialog;

public class SearchDialog {

    public static String findText = "";
    public static int phraseNumber;
    public static int selStart;

    private static DocumentViewerActivity documentViewer;
    private static String[] languages = new String[2];
    private static int curLangIndex, selectedLangIndex;
    private static String substring;
    private static int curPosition;
    private static int searchFrom = R.id.rbForwardFromCurrent;
    private static boolean useCase = false;
    private static boolean wholeWord = false;

    public static String langId() {
        return languages[selectedLangIndex];
    }

    public static void open(final DocumentViewerActivity documentViewer) {
        SearchDialog.documentViewer = documentViewer;
        AlertDialog.Builder builder = new AlertDialog.Builder(documentViewer);
        builder.setIcon(R.drawable.menu_search);
        builder.setTitle(R.string.menu_search);

        final View dialogView = documentViewer.getLayoutInflater().inflate(R.layout.search_dialog, null);
        builder.setView(dialogView);

        final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
        editText.setText(findText);

        final CheckBox checkBoxUseCase = (CheckBox) dialogView.findViewById(R.id.checkBoxUseCase);
        checkBoxUseCase.setChecked(useCase);

        final CheckBox checkBoxWholeWord = (CheckBox) dialogView.findViewById(R.id.checkBoxWholeWord);
        checkBoxWholeWord.setChecked(wholeWord);

        final RadioGroup radioGroup = (RadioGroup) dialogView.findViewById(R.id.radioGroup);
        radioGroup.check(searchFrom);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager imm = (InputMethodManager)
                    editText.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                findText = editText.getText().toString();
                useCase = checkBoxUseCase.isChecked();
                wholeWord = checkBoxWholeWord.isChecked();
                searchFrom = radioGroup.getCheckedRadioButtonId();

                startSearch();
            }
        }); // setPositiveButton

        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(s.length() != 0);
            }
        });

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(editText.getText().length() != 0);
    }  // open

    private static void startSearch() {
        switch (searchFrom) {
            case R.id.rbForwardFromCurrent: phraseNumber = DocumentViewerActivity.document.currentPhrase; break;
            case R.id.rbBackwardFromCurrent: phraseNumber = DocumentViewerActivity.document.currentPhrase - 1; break;
            case R.id.rbForwardFromBegin: phraseNumber = 0; break;
            case R.id.rbBackwardFromEnd: phraseNumber = documentViewer.phrases.size() - 1; break;
        }

        languages[0] = Settings.primaryLanguage();
        languages[1] = Settings.secondaryLanguage();
        curPosition = -1;
        boolean forward = (searchFrom == R.id.rbForwardFromCurrent || searchFrom == R.id.rbForwardFromBegin);

        if (forward)
            curLangIndex = 0;
        else
            curLangIndex = 1;

        if (useCase)
            substring = findText;
        else
            substring = findText.toUpperCase();

        documentViewer.updateSearchBar();

        new Finder().execute(forward);
    } // startSearch

    private static void showNotFound() {
        Dialog.showMessage(
            documentViewer,
            documentViewer.getString(R.string.menu_search),
            findText + " - " + documentViewer.getString(R.string.text_not_found));
    }

    public static void search(boolean forward) {
        if (phraseNumber < DocumentViewerActivity.document.currentPhrase ||
            phraseNumber >= DocumentViewerActivity.document.currentPhrase + Settings.phrasesOnPage())
        {
            curPosition = -1;
            if (forward) {
                phraseNumber = DocumentViewerActivity.document.currentPhrase;
                curLangIndex = 0;
            }
            else {
                phraseNumber = DocumentViewerActivity.document.currentPhrase - 1;
                curLangIndex = 1;
            }
        }
        else {
            curPosition = selStart;
            curLangIndex = selectedLangIndex;
        }

        new Finder().execute(forward);
    } // search

    private static class Finder extends AsyncTask<Boolean, Integer, Boolean> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute () {
            progressDialog = new ProgressDialog(SearchDialog.documentViewer);
            progressDialog.setMessage(SearchDialog.documentViewer.getString(R.string.menu_search) + " " + findText);
            progressDialog.setProgressNumberFormat(
                SearchDialog.documentViewer.getString(R.string.phrase) + " %1d/%2d");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(documentViewer.phrasesCount());
            progressDialog.setProgress(0);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Finder.this.cancel(true);
                }
            });

            (new Handler()).postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (Finder.this.getStatus() == AsyncTask.Status.RUNNING)
                            progressDialog.show();
                    }
                },
                500
            ); // post
        } // onPreExecute

        @Override
        protected Boolean doInBackground(Boolean... params) {
            if (params[0])
                return searchNext();
            else
                return searchPrevious();
        } // doInBackground

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();

            if (result) {
                documentViewer.showSearchBar();
                (new Handler()).post(
                    new Runnable() {
                        @Override
                        public void run() {
                            documentViewer.selectFounded();
                        }
                    }
                ); // post
            }
            else
                showNotFound();
        } // onPostExecute

        private boolean searchNext() {
            for (int i = phraseNumber; i < documentViewer.phrases.size(); i++) {
                for (int langIndex = curLangIndex; langIndex < 2; langIndex++) {
                    if (searchInPhrase(i, langIndex, true)) return true;
                } // for
                curLangIndex = 0;
            } // for

            return false;
        } // searchNext

        private boolean searchPrevious() {
            for (int i = phraseNumber; i >= 0; i--) {
                for (int langIndex = curLangIndex; langIndex >= 0; langIndex--) {
                    if (curPosition == 0) {
                        curPosition = -1;
                        break;
                    }
                    if (searchInPhrase(i, langIndex, false)) return true;
                } // for
                curLangIndex = 1;
            } // for

            return false;
        } // searchPrevious

        private boolean searchInPhrase(int phraseNumber, int langIndex, boolean forward) {
            Spanned sPhrase = documentViewer.getPhrase(phraseNumber, languages[langIndex]);
            publishProgress(phraseNumber);
            if (sPhrase == null) return false;
            String phrase = sPhrase.toString();
            if (!useCase) phrase = phrase.toUpperCase();
            if (forward)
                curPosition = phrase.indexOf(substring, curPosition + 1);
            else {
                if (curPosition == -1)
                    curPosition = phrase.lastIndexOf(substring);
                else
                    curPosition = phrase.lastIndexOf(substring, curPosition - 1);
            }

            if (curPosition != -1) {
                if (wholeWord && !checkForWholeWord(phrase)) {
                    curPosition = -1;
                    return false;
                }
                SearchDialog.phraseNumber = phraseNumber;
                selStart = curPosition;
                selectedLangIndex = langIndex;
                return true;
            }
            else
                return false;
        } // searchInPhrase

        private boolean checkForWholeWord(String phrase) {
            int right = curPosition + substring.length();
            return
                (curPosition == 0 || Character.isWhitespace(phrase.charAt(curPosition - 1)))
                    &&
                    (right == phrase.length() || Character.isWhitespace(phrase.charAt(right)));
        }
    } // Finder
}
