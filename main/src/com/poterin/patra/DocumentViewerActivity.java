package com.poterin.patra;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class DocumentViewerActivity extends FragmentActivity implements Menuer.MenuSelector {

    public static DocumentViewerActivity documentViewerActivity;
    public static BookHistory.DocumentMenuItem document;
    public static final int RESULT_EXCEPTION = RESULT_FIRST_USER + 1;
    public static final int RESULT_EXIT = RESULT_EXCEPTION + 1;
    public static final int RESULT_REOPEN = RESULT_EXIT + 1;

    public static Speaker speaker;
    private Element rootElement;
    public ArrayList<Element> phrases;
    private TextView textViewDocument, textViewCurrentPage;
    private DocumentViewerAdapter documentViewerAdapter;
    public ViewPager viewPager;
    private LinearLayout layoutSearch;
    private DocumentPage[] pageCache;
    private ProgressBar progressBar;
    public int textColor;
    public int actionOverflowId;
    private static Document previousDocument;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Settings.isNightMode())
            setTheme(R.style.thin_action_bar_dark);
        else
            setTheme(R.style.thin_action_bar_light);
        setContentView(R.layout.document_viewer);

        documentViewerActivity = this;
        speaker = new Speaker(this);

        textViewCurrentPage = (TextView) findViewById(R.id.textViewCurrentPage);
        textViewCurrentPage.setText("");
        textViewDocument = (TextView) findViewById(R.id.textViewDocument);
        textViewDocument.setText("");

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) textViewDocument.getLayoutParams();
        lp.leftMargin = Utils.ptToPixels(Settings.leftMargin());
        textViewDocument.setLayoutParams(lp);

        layoutSearch = (LinearLayout) findViewById(R.id.layoutSearch);
        layoutSearch.setVisibility(View.GONE);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        setDayNight(Settings.isNightMode());

        if (previousDocument == null)
            (new DocumentLoader()).execute();
        else
            onDocumentLoaded(previousDocument);
    } // onCreate

    @Override
    public void finish() {
        documentViewerActivity = null;
        speaker.shutdown();
        super.finish();
    }

    @Override
    protected void onPause() {
        MainActivity.saveDocuments();
        super.onPause();
    }

    public static void updateCachedPages() {
        if (documentViewerActivity == null) return;

        int i = documentViewerActivity.viewPager.getCurrentItem();
        if (i > 0) documentViewerActivity.pageCache[i - 1].update();
        documentViewerActivity.pageCache[i].update();
        if (i < documentViewerActivity.pageCache.length - 1) documentViewerActivity.pageCache[i + 1].update();
    }

    public void showSearchBar() {
        layoutSearch.setVisibility(View.VISIBLE);

        final ImageButton buttonRight = (ImageButton) layoutSearch.findViewById(R.id.buttonRight);
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchDialog.search(true);
            }
        });

        ImageButton buttonLeft = (ImageButton) layoutSearch.findViewById(R.id.buttonLeft);
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchDialog.search(false);
            }
        });

        ImageButton buttonClose = (ImageButton) layoutSearch.findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutSearch.setVisibility(View.GONE);
            }
        });
    } // showSearchBar

    public void updateSearchBar() {
        TextView textViewFindText = (TextView) layoutSearch.findViewById(R.id.textViewFindText);
        textViewFindText.setText(SearchDialog.findText);
    }

    public void selectFounded() {
        viewPager.setCurrentItem(SearchDialog.phraseNumber / Settings.phrasesOnPage());
        currentDocumentPage().selectFounded();
    }

    public Spanned getPhrase(int index, String langId)  {
        NodeList childs = phrases.get(index).getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {
            if (childs.item(i).getNodeName().equals(langId)) {
                return Html.fromHtml(childs.item(i).getTextContent());
            }
        }

        return null;
    }

    public void setTranslationPhrase(int index, String phrase)  {
        Element element = rootElement.getOwnerDocument().createElement(Settings.secondaryLanguage());
        element.setTextContent(phrase);
        phrases.get(index).appendChild(element);
    }

    public DocumentPage currentDocumentPage() {
        return pageCache[viewPager.getCurrentItem()];
    }

    private void onDocumentLoaded(Document xmlData) {
        previousDocument = null;
        rootElement = xmlData.getDocumentElement();

        if (!rootElement.getNodeName().equals("multum_lingua")) {
            setException(getString(R.string.incorrect_document));
            return;
        }

        if (!rootElement.getAttribute("version").equals("2")) {
            setException(getString(R.string.incorrect_document_version));
            return;
        }

        phrases = new ArrayList<Element>();
        NodeList childs = rootElement.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            if (childs.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) childs.item(i);
                if (element.getNodeName().equals("information"))
                    parseDocumentSettings(element);
                else
                    phrases.add(element);
            }
        } // for

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setPageMargin(10);
        documentViewerAdapter = new DocumentViewerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(documentViewerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {}

            @Override
            public void onPageSelected(int i) {
                document.currentPhrase = i * Settings.phrasesOnPage();
                setPageInfo(i + 1);
            }

            @Override
            public void onPageScrollStateChanged(int i) {}
        });

        viewPager.setCurrentItem(document.currentPhrase / Settings.phrasesOnPage());
        setPageInfo(viewPager.getCurrentItem() + 1);
    } // onDocumentLoaded

    public int phrasesCount() {
        return phrases.size();
    }

    private void setException(String message) {
        Intent intent = new Intent();
        intent.putExtra("message", message);
        setResult(RESULT_EXCEPTION, intent);
        finish();
    }

    private void parseDocumentSettings(Element settings) {
        textViewDocument.setText(Utils.getBookTitle(this, settings));
        document.caption = textViewDocument.getText().toString();
    } // parseDocumentSettings

    private void setPageInfo(int page) {
        textViewCurrentPage.setText(page + "/" + documentViewerAdapter.getCount());
        progressBar.setProgress(100 * page / documentViewerAdapter.getCount());
    } // setAdapter

    public void onBookmarkSelect(Element bookmark) {
        viewPager.setCurrentItem(Integer.valueOf(bookmark.getAttribute("phrase")) / Settings.phrasesOnPage());
    } // onBookmarkSelect

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        if (requestCode == Settings.REQUEST_ID && resultCode == Activity.RESULT_OK)
            reopenDocument();
    }

    private void setDayNight(boolean isNightMode) {
        LinearLayout layoutContent = (LinearLayout) findViewById(R.id.layoutContent);
        layoutContent.setBackgroundColor(Color.parseColor(isNightMode ? "#000000" : "#F8F6E8"));
        textColor = Color.parseColor(isNightMode ? "#999999" : "#000000");
        int actionBarColor = Color.parseColor(isNightMode ? "#33322F" : "#E5E3D7");
        LinearLayout layoutActionBar = (LinearLayout) findViewById(R.id.layoutActionBar);
        layoutActionBar.setBackgroundColor(actionBarColor);
        layoutSearch.setBackgroundColor(actionBarColor);
        textViewDocument.setTextColor(textColor);
        textViewCurrentPage.setTextColor(textColor);
        ((TextView) layoutSearch.findViewById(R.id.textViewFindText)).setTextColor(textColor);
        actionOverflowId = isNightMode ? R.drawable.ic_action_overflow_light : R.drawable.ic_action_overflow;
        ImageButton imageButtonMenu = (ImageButton) findViewById(R.id.imageButtonMenu);
        imageButtonMenu.setImageResource(actionOverflowId);
    } // setDayNight

    private void switchDayNight() {
        Settings.settingsEditor.putBoolean("isNightMode", !Settings.isNightMode());
        Settings.settingsEditor.commit();
        MainActivity.setActualTheme();
        reopenDocument();
    }

    private void reopenDocument() {
        previousDocument = rootElement.getOwnerDocument();
        setResult(RESULT_REOPEN);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!Settings.scrollOnVolumeKey()) return super.dispatchKeyEvent(event);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (viewPager.getCurrentItem() > 0)
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (viewPager.getCurrentItem() < documentViewerAdapter.getCount() - 1)
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                    return true;
            }
        }
        else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
               return true;
        }

        return super.dispatchKeyEvent(event);
    } // dispatchKeyEvent

    @Override
    public void onMenuItemSelect(String menuId) {
        switch (menuId) {
            case "scroll": ScrollDialog.open(this); break;
            case "bookmarks": Bookmarks.open(this); break;
            case "search": SearchDialog.open(this); break;
            case "day_night": switchDayNight(); break;
            case "settings": Settings.open(this); break;
            case "my_dictionary": startActivity(new Intent(this, DictionaryActivity.class)); break;
            case "exit":
                setResult(RESULT_EXIT);
                finish();
        }
    } // onMenuItemSelect

    public void onMenuClick(View view) {
        new Menuer(
            findViewById(R.id.imageButtonMenu),
            new String[] {"scroll", "bookmarks", "search", "my_dictionary", "settings", "day_night", "exit"},
            this,
            R.dimen.action_bar_menu_width);
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                onMenuClick(null);
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    private class DocumentViewerAdapter extends FragmentPagerAdapter {

        public DocumentViewerAdapter(FragmentManager fm) {
            super(fm);
            pageCache = new DocumentPage[getCount()];
        }

        @Override
        public int getCount() {
            return (phrases.size() - 1) / Settings.phrasesOnPage() + 1;
        }

        @Override
        public Fragment getItem(int position) {
            pageCache[position] = new DocumentPage(DocumentViewerActivity.this, position);
            return pageCache[position];
        }
    }

    private class DocumentLoader extends AsyncTask<Void, Void, Document> {
        private Exception raisedException = null;
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute () {
            progressDialog = new ProgressDialog(DocumentViewerActivity.this);
            progressDialog.setMessage(getString(R.string.loading_book));
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    DocumentLoader.this.cancel(true);
                    DocumentViewerActivity.this.finish();
                }
            });
            progressDialog.show();
        } // onPreExecute

        @Override
        protected Document doInBackground(Void... params) {
            try {
                if (document.isTest)
                    return XMLUtil.parseStream(getAssets().open(document.fileName));
                else
                    return XMLUtil.parseFile(document.fileName);
            } catch (Exception e) {
                raisedException = e;
                return null;
            }
        } // doInBackground

        @Override
        protected void onPostExecute(Document result) {
            progressDialog.dismiss();
            if (raisedException == null) {
                onDocumentLoaded(result);
            }
            else {
                if (raisedException instanceof FileNotFoundException)
                    setException(String.format(getString(R.string.file_not_found), document.fileName));
                else
                {
                    Utils.logException(raisedException);
                    setException(getString(R.string.cannot_open_book) + "\n\n" + raisedException.toString());
                }
            }
        }

    } // DocumentLoader
}