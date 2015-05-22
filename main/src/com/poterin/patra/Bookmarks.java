package com.poterin.patra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Bookmarks {

    private static DocumentViewerActivity documentViewer;

    private static Element rootElement() {
        return DocumentViewerActivity.document.bookmarks;
    }

    private static Document ownerDocument() {
        return rootElement().getOwnerDocument();
    }

    private static NodeList bookmarks() {
        return rootElement().getChildNodes();
    }

    private static Element get(int index) {
        return (Element) bookmarks().item(index);
    }

    private static boolean hasBookmark(int phraseNumber) {
        for (int i= 0; i < bookmarks().getLength(); i++) {
            if (Integer.valueOf(get(i).getAttribute("phrase")) == phraseNumber) {
                return true;
            }
        }
        return false;
    }

    private static Element addCandidate(DocumentViewerActivity documentViewer, int phraseNumber) {
        Bookmarks.documentViewer = documentViewer;

        int currentPhraseIndex = phraseNumber;

        if (currentPhraseIndex == -1) {
            int currentPage = documentViewer.viewPager.getCurrentItem();
            currentPhraseIndex = currentPage * Settings.phrasesOnPage();
        }

        DocumentPage currentDocumentPage = documentViewer.currentDocumentPage();
        String currentPhrase = currentDocumentPage.getPhrase(currentPhraseIndex, Settings.primaryLanguage()).toString();
        if (currentPhrase.length() > 62)
            currentPhrase = currentPhrase.substring(0, 60) + "...";

        Element element = ownerDocument().createElement("bookmark");
        element.setAttribute("title", currentPhrase);
        element.setAttribute("phrase", String.valueOf(currentPhraseIndex));
        element.setAttribute("candidate", Boolean.toString(true));

        if (bookmarks().getLength() > 0)
            rootElement().insertBefore(element, bookmarks().item(0));
        else
            rootElement().appendChild(element);

        return element;
    } // addCandidate

    public static void open(final DocumentViewerActivity documentViewer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(documentViewer);
        builder.setIcon(R.drawable.menu_bookmarks);
        builder.setTitle(R.string.menu_bookmarks);

        addCandidate(documentViewer, -1);

        View dialogView = documentViewer.getLayoutInflater().inflate(R.layout.bookmarks, null);
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.close, null);
        builder.setNeutralButton(R.string.menu_settings, null);

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ListView listView = (ListView) dialogView.findViewById(R.id.listView);
        listView.setAdapter(new BookmarksAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (get(i).hasAttribute("candidate"))
                    addBookmark(get(i));
                else {
                    documentViewer.onBookmarkSelect(get(i));
                    if (Settings.deleteBookmarkAfterUse)
                        rootElement().removeChild(get(i));
                }
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                for (int i = 0; i < bookmarks().getLength(); i++) {
                    if (get(i).hasAttribute("candidate")) {
                        rootElement().removeChild(get(i));
                        return;
                    }
                }
            }
        });

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        }); // setOnClickListener
    } // open

    private static void openSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(documentViewer);
        builder.setIcon(R.drawable.menu_bookmarks);
        builder.setTitle(R.string.menu_settings);

        View dialogView = documentViewer.getLayoutInflater().inflate(R.layout.bookmarks_settings, null);
        builder.setView(dialogView);

        final CheckBox checkBoxDeleteBookmarkAfterUse =
            (CheckBox) dialogView.findViewById(R.id.checkBoxDeleteBookmarkAfterUse);
        checkBoxDeleteBookmarkAfterUse.setChecked(Settings.deleteBookmarkAfterUse);

        final CheckBox checkBoxDeleteBookmarksAfterInsert =
            (CheckBox) dialogView.findViewById(R.id.checkBoxDeleteBookmarksAfterInsert);
        checkBoxDeleteBookmarksAfterInsert.setChecked(Settings.deleteBookmarksAfterInsert);

        builder.setNegativeButton(R.string.cancel, null);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.deleteBookmarkAfterUse = checkBoxDeleteBookmarkAfterUse.isChecked();
                Settings.settingsEditor.putBoolean("deleteBookmarkAfterUse", Settings.deleteBookmarkAfterUse);

                Settings.deleteBookmarksAfterInsert = checkBoxDeleteBookmarksAfterInsert.isChecked();
                Settings.settingsEditor.putBoolean("deleteBookmarksAfterInsert", Settings.deleteBookmarksAfterInsert);

                Settings.settingsEditor.commit();
            }
        }); // setPositiveButton

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.show();
    } // openSettings

    public static void addBookmark(DocumentViewerActivity documentViewer, int phraseNumber) {
        addBookmark(addCandidate(documentViewer, phraseNumber));
    } // addBookmark

    private static void addBookmark(Element bookmark) {
        bookmark.removeAttribute("candidate");

        if (Settings.deleteBookmarksAfterInsert) {
            for (int i = bookmarks().getLength() - 1; i > 0 ; i--) {
                if (get(i) != bookmark) {
                    rootElement().removeChild(get(i));
                }
            }
        } // if

        Toast toast = Toast.makeText(documentViewer, R.string.bookmark_is_added, Toast.LENGTH_SHORT);
        toast.show();
    }

    private static class BookmarksAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return bookmarks().getLength();
        }

        @Override
        public long getItemId (int position) {
            return 0;
        }

        @Override
        public Object getItem (int position) {
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =
                (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.bookmark_item, parent, false);

            int index = position;

            final Element curBookmark = get(index);

            int currentPhrase = Integer.valueOf(curBookmark.getAttribute("phrase"));
            float percent = Math.round((float) currentPhrase / documentViewer.phrasesCount() * 1000) / 10f;

            final TextView textViewPosition = (TextView) rowView.findViewById(R.id.textViewPosition);
            textViewPosition.setText(
                String.format(documentViewer.getString(R.string.bookmark_position), percent, currentPhrase + 1));

            final TextView textViewTitle = (TextView) rowView.findViewById(R.id.textViewTitle);
            textViewTitle.setText(curBookmark.getAttribute("title"));

            final ImageButton buttonBookmarkAction = (ImageButton) rowView.findViewById(R.id.buttonBookmarkAction);
            if (Boolean.valueOf(curBookmark.getAttribute("candidate")))
                buttonBookmarkAction.setImageResource(R.drawable.add);

            buttonBookmarkAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Boolean.valueOf(curBookmark.getAttribute("candidate"))) {
                        buttonBookmarkAction.setImageResource(R.drawable.delete);
                        addBookmark(curBookmark);
                    }
                    else {
                        removeBookmarkDialog(
                            curBookmark,
                            textViewPosition.getText().toString() + "\n\n" + textViewTitle.getText().toString());
                    }
                }
            });

            return rowView;
        } // getView

        private void removeBookmarkDialog(final Element bookmark, final String message) {
            AlertDialog.Builder builder =
                new AlertDialog.Builder(documentViewer);
            builder.setIcon(R.drawable.delete_bookmark);
            builder.setTitle(R.string.delete_bookmark);
            builder.setMessage(message);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    rootElement().removeChild(bookmark);
                    notifyDataSetChanged();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            alert.show();
        } // removeBookmarkDialog
    }  // BookmarksAdapter
}
