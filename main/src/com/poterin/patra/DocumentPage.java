package com.poterin.patra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import org.w3c.dom.Element;

import java.io.IOException;
import static com.poterin.patra.UserDictionary.userDictionary;

public class DocumentPage extends Fragment implements Menuer.MenuSelector {

    private int currentPage;
    private int currentPhrase;
    private PageViewerAdapter pageViewerAdapter;
    private ExpandableListView listView;
    private DocumentViewerActivity documentViewer;
    private ImageButton activeButtonMute;

    public DocumentPage(DocumentViewerActivity documentViewer, int currentPage) {
        this.documentViewer = documentViewer;
        this.currentPage = currentPage;

        pageViewerAdapter = new PageViewerAdapter();
    }

    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public Spanned getPhrase(int index, String langId)  {
        Spanned result = documentViewer.getPhrase(index, langId);

        if (result == null)
            return new SpannedString(String.format(
                getString(R.string.undefined_phrase), Languages.getLanguageName(langId)));
        else
            return result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.document_page, container, false);

        listView = (ExpandableListView) view.findViewById(R.id.expandableListView);
        listView.setAdapter(pageViewerAdapter);
        listView.setOnTouchListener(onTouchListener);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) listView.getLayoutParams();
        lp.leftMargin = Utils.ptToPixels(Settings.leftMargin());
        lp.rightMargin = Utils.ptToPixels(Settings.rightMargin());
        lp.topMargin = Utils.ptToPixels(Settings.topMargin());
        lp.bottomMargin = Utils.ptToPixels(Settings.bottomMargin());
        listView.setLayoutParams(lp);

        return view;
    } // onCreateView

    public void update() {
        pageViewerAdapter.notifyDataSetChanged();
    }

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {

        private void checkForScroll(final View view, final MotionEvent motionEvent) {
            DisplayMetrics metrics = documentViewer.getResources().getDisplayMetrics();
            int sensitiveWidth = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 10, metrics));

            if (motionEvent.getX() < sensitiveWidth && currentPage > 0) {
                documentViewer.viewPager.setCurrentItem(currentPage - 1);
            } else {
                if (motionEvent.getX() > view.getWidth() - sensitiveWidth
                    && currentPage < documentViewer.viewPager.getAdapter().getCount() - 1)
                {
                    documentViewer.viewPager.setCurrentItem(currentPage + 1);
                }
            }
        } // checkForScroll

        @Override
        public boolean onTouch(final View view, final MotionEvent motionEvent) {
            boolean hasSelection = view instanceof TextView && ((TextView) view).hasSelection();

            if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP && !hasSelection && Settings.scrollOnTap()) {
                checkForScroll(view, motionEvent);
            }

            return false;
        } // onTouch
    }; // onTouchListener

    public void selectFounded() {
        final int index = SearchDialog.phraseNumber - currentPage * Settings.phrasesOnPage();

        if (SearchDialog.langId().equals(Settings.primaryLanguage()))
            listView.setSelection(index);
        else {
            listView.expandGroup(index);
            listView.setSelectedChild(index, 0, true);
        }

        listView.post(
            new Runnable() {
                @Override
                public void run() {
                    TextView textViewPhrase;
                    if (SearchDialog.langId().equals(Settings.primaryLanguage()))
                        textViewPhrase = pageViewerAdapter.phrasesTextView[index];
                    else
                        textViewPhrase = pageViewerAdapter.translationTextView[index];

                    Selection.setSelection((Spannable) textViewPhrase.getText(), 0, 0);
                    Selection.setSelection(
                        (Spannable) textViewPhrase.getText(),
                        SearchDialog.selStart,
                        SearchDialog.selStart + SearchDialog.findText.length());
                    // textViewPhrase.setSelected(true);
                    textViewPhrase.requestFocus();
                }
            }
        ); // post
    } // selectFounded

    @Override
    public void onMenuItemSelect(String menuId) {
        switch (menuId) {
            case "add_bookmark":
                Bookmarks.addBookmark(documentViewer, currentPhrase);
                break;

            case "voice":
                documentViewer.speaker.speakOut(
                    pageViewerAdapter.phrasesTextView[currentPhrase - currentPage * Settings.phrasesOnPage()].
                        getText().toString(),
                    activeButtonMute);
                break;
        }
    } // onMenuItemSelect

    private void setActiveButtonMute(View parentView) {
        int buttonMuteSize = Math.round(((TextView) parentView.findViewById(R.id.textViewPhrase)).getTextSize());
        activeButtonMute = (ImageButton) parentView.findViewById(R.id.buttonMute);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(buttonMuteSize, buttonMuteSize);
        activeButtonMute.setLayoutParams(lp);
    }

    private class PageViewerAdapter extends BaseExpandableListAdapter {

        private TextView[] phrasesTextView, translationTextView;

        private PageViewerAdapter() {
            phrasesTextView = new TextView[getGroupCount()];
            translationTextView = new TextView[getGroupCount()];
        }

        @Override
        public int getGroupCount() {
            if ((currentPage + 1) * Settings.phrasesOnPage() > documentViewer.phrasesCount())
                return documentViewer.phrasesCount() - currentPage * Settings.phrasesOnPage();
            else
               return Settings.phrasesOnPage();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View rowView = inflater.inflate(R.layout.phrase, parent, false);

            final int index = currentPage * Settings.phrasesOnPage() + groupPosition;

            final TextView textViewPhrase = (TextView) rowView.findViewById(R.id.textViewPhrase);
            phrasesTextView[groupPosition] = textViewPhrase;
            textViewPhrase.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize());
            textViewPhrase.setTextColor(documentViewer.textColor);
            textViewPhrase.setText(getPhrase(index, Settings.primaryLanguage()), TextView.BufferType.SPANNABLE);
            textViewPhrase.setCustomSelectionActionModeCallback(new TextViewSelectCallback(textViewPhrase));
            textViewPhrase.setOnTouchListener(onTouchListener);

            final TextView textViewPhraseNumber = (TextView) rowView.findViewById(R.id.textViewPhraseNumber);
            textViewPhraseNumber.setTextSize(Settings.fontSize() - 4);
            textViewPhraseNumber.setText("#" + String.valueOf(index + 1));

            final TextView textViewTranslation = (TextView) rowView.findViewById(R.id.textViewTranslation);
            textViewTranslation.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize());
            textViewTranslation.setText(Languages.getLanguageName(Settings.secondaryLanguage()));
            textViewTranslation.setId(groupPosition);
            textViewTranslation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (listView.isGroupExpanded(v.getId()))
                        listView.collapseGroup(v.getId());
                    else {
                        listView.expandGroup(v.getId());
                        if (v.getId() == getGroupCount() - 1) listView.smoothScrollByOffset(1);
                    }
                }
            });

            View layoutPhraseNumber = rowView.findViewById(R.id.layoutPhraseNumber);
            layoutPhraseNumber.setId(index);
            layoutPhraseNumber.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentPhrase = v.getId();
                    setActiveButtonMute(rowView);
                    new Menuer(
                        textViewPhraseNumber,
                        new String[] {"add_bookmark", "voice"},
                        DocumentPage.this,
                        R.dimen.phrase_menu_width);
                }
            });

            ImageView imageViewActionOverflow = (ImageView) rowView.findViewById(R.id.imageViewActionOverflow);
            imageViewActionOverflow.setImageResource(documentViewer.actionOverflowId);

            return rowView;
        } // getGroupView

        @Override
        public View getChildView(
            int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater =
                (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View rowView = inflater.inflate(R.layout.trans_phrase, parent, false);

            int index = currentPage * Settings.phrasesOnPage() + groupPosition;

            TextView textViewPhrase = (TextView) rowView.findViewById(R.id.textViewPhrase);
            translationTextView[groupPosition] = textViewPhrase;
            textViewPhrase.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize());
            setTranslation(index, textViewPhrase);
            textViewPhrase.setOnTouchListener(onTouchListener);

            return rowView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        private void setTranslation(int phraseIndex, TextView textViewTranslation) {
            if (!Settings.translateByYandex()) {
                textViewTranslation.setText(
                    getPhrase(phraseIndex, Settings.secondaryLanguage()), TextView.BufferType.SPANNABLE);
                return;
            }

            Spanned result = documentViewer.getPhrase(phraseIndex, Settings.secondaryLanguage());

            if (result == null)
                new TranslatorTask(phraseIndex, textViewTranslation);
            else
                textViewTranslation.setText(result, TextView.BufferType.SPANNABLE);
        }
    } // PageViewerAdapter

    private class TextViewSelectCallback implements ActionMode.Callback {
    // http://stackoverflow.com/questions/12995439/custom-cut-copy-action-bar-for-edittext-that-shows-text-selection-handles
        private TextView textView;

        public TextViewSelectCallback(TextView textView) {
            this.textView = textView;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.selected_calback, menu);
            menu.findItem(R.id.menuTranslate).setIcon(
                documentViewer.getResources().getIdentifier(
                    "ic_" + Settings.defaultDictionary(),
                    "drawable",
                    documentViewer.getPackageName()));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Element item = userDictionary.findItem(selectedText());
            if (item != null) {
                Toast toast = Toast.makeText(
                    textView.getContext(),
                    textView.getContext().getString(R.string.found_translation, item.getAttribute("translation")),
                    Toast.LENGTH_LONG);
                toast.show();
            }
            return true;
        }

        private String selectedText() {
            return textView.getText().toString().substring(textView.getSelectionStart(), textView.getSelectionEnd());
        }

        private void selectTranslator(final String text) {
            AlertDialog.Builder builder = new AlertDialog.Builder(documentViewer);
            builder.setTitle(text);

            builder.setAdapter(
                new ArrayAdapter<>(
                    documentViewer,
                    android.R.layout.simple_list_item_1,
                    documentViewer.getResources().getStringArray(R.array.dictionaries)),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Translator.translate(documentViewer, text, Translator.dictionariesIds[which]);
                    }
                }); // setAdapter

            builder.setNegativeButton(android.R.string.cancel, null);

            builder.show();
        } // selectTranslator

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menuTranslate:
                    Translator.translate(textView.getContext(), selectedText());
                    mode.finish();
                    return true;

                case R.id.menuVoice:
                    setActiveButtonMute((View) textView.getParent());
                    documentViewer.speaker.speakOut(selectedText(), activeButtonMute);
                    mode.finish();
                    return true;

                case R.id.menuSelectTranslator:
                    selectTranslator(selectedText());
                    return true;
            } // switch

            return false;
        } // onActionItemClicked

        @Override
        public void onDestroyActionMode(ActionMode mode) {}
    } // TextViewSelectCallback

    private class TranslatorTask extends Translator.YandexTaskBase {
        private final int phraseIndex;
        private final TextView textViewTranslation;

        private TranslatorTask(int phraseIndex, TextView textViewTranslation) {
            super(getPhrase(phraseIndex, Settings.primaryLanguage()).toString(), false);
            this.phraseIndex = phraseIndex;
            this.textViewTranslation = textViewTranslation;
            textViewTranslation.setText(R.string.yandex_translating);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            String translation = null;

            try {
                if (raisedException == null) translation = getTranslation(result);
            } catch (Exception e) {
                raisedException = e;
            }

            if (raisedException == null) {
                documentViewer.setTranslationPhrase(phraseIndex, translation);
                textViewTranslation.setText(
                    getPhrase(phraseIndex, Settings.secondaryLanguage()), TextView.BufferType.SPANNABLE);
            }
            else {
                Utils.logException(raisedException);
                if (raisedException instanceof IOException)
                    textViewTranslation.setText(R.string.yandex_translate_error);
                else
                    textViewTranslation.setText(raisedException.toString());
            }
        } // onPostExecute
    } // TranslatorTask
}
