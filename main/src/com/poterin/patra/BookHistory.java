package com.poterin.patra;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BookHistory {

    private final MainMenuList mainMenuList;
    private final Document storeDocument;
    public final List<DocumentMenuItem> bookItems;

    public BookHistory(final MainMenuList mainMenuList) throws Exception {
        this.mainMenuList = mainMenuList;
        bookItems = new ArrayList<DocumentMenuItem>();
        storeDocument = XMLUtil.parseString(Settings.preferences.getString("documents", "<documents/>"));

        loadDocuments();
    } // BookHistory

    private MainActivity mainActivity() {
        return mainMenuList.mainActivity;
    }

    private NodeList books() {
        return storeDocument.getDocumentElement().getChildNodes();
    }

    private DocumentMenuItem getOrCreateDocument(String fileName) throws Exception {
        DocumentMenuItem result = findDocumentByFileName(fileName);

        if (result != null)
            return result;
        else
            return new DocumentMenuItem(fileName);
    } // getOrCreateDocument

    public DocumentMenuItem findDocumentByFileName(String fileName) {
        for (DocumentMenuItem menuItem : bookItems) {
            if (menuItem.fileName.equals(fileName))
                return menuItem;
        } // for

        return null;
    } // getOrCreateDocument

    private void createTestDocument(String fileName, String caption) throws Exception {
        DocumentMenuItem testDocument = getOrCreateDocument(fileName);
        testDocument.caption = caption;
        testDocument.isTest = true;
    }

    private void loadDocuments() throws Exception {
        if (books().getLength() == 0) {
            createTestDocument("three_men_in_a_boat.mlb", "Three Men in a Boat /Jerome K. Jerome/");
            createTestDocument("double_dyed_deceiver_ohenry.mlb", "A Double-Dyed Deceiver /O.Henry/");
        }
        else {
            for (int i = 0; i < books().getLength(); i++) {
                new DocumentMenuItem((Element) books().item(i));
            }
        } // if
    } // loadDocuments

    public void saveDocuments() {
        for (DocumentMenuItem menuItem : bookItems) menuItem.saveToXML();

        try {
            Settings.settingsEditor.putString("documents", XMLUtil.domToString(storeDocument, false));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Settings.settingsEditor.commit();
    }

    public void openDocument(String fileName) {
        try {
            DocumentMenuItem documentMenuItem = getOrCreateDocument(fileName);
            mainMenuList.notifyDataSetChanged();
            documentMenuItem.onItemClick();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void fillBookView(int index, View view) {
        DocumentMenuItem menuItem = bookItems.get(index);

        TextView textView = (TextView) view.findViewById(R.id.label);
        ImageView imageView = (ImageView) view.findViewById(R.id.logo);

        textView.setText(menuItem.caption);
        imageView.setImageBitmap(BitmapFactory.decodeResource(mainActivity().getResources(), R.drawable.book));

        ImageButton buttonDeleteDocument = (ImageButton) view.findViewById(R.id.buttonDeleteDocument);
        buttonDeleteDocument.setId(index);
        buttonDeleteDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDocumentDialog(view.getId());
            }
        });
    }

    private void deleteDocumentDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity());
        builder.setTitle(R.string.app_name);
        builder.setMessage(
            String.format(mainActivity().getString(R.string.delete_document), (bookItems.get(index).caption)));
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                bookItems.get(index).remove();
                bookItems.remove(index);
                mainMenuList.notifyDataSetChanged();
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
    } // deleteDocumentDialog

    public class DocumentMenuItem {
        public final String fileName;
        public int currentPhrase = 0;
        public boolean isTest = false;
        public final Element bookmarks;
        public String caption;
        private final Element xml;

        private DocumentMenuItem(final Element xml) throws Exception {
            this.xml = xml;

            fileName = xml.getAttribute("file");
            isTest = Boolean.valueOf(xml.getAttribute("isTest"));
            caption = xml.getAttribute("caption");
            currentPhrase = Integer.valueOf(xml.getAttribute("currentPhrase"));

            if (xml.hasChildNodes())
                bookmarks = (Element) xml.getFirstChild();
            else
                // Drop this line after 01.01.2015
                bookmarks = (Element) xml.appendChild(storeDocument.createElement("bookmarks"));

            bookItems.add(0, this);
        } // DocumentMenuItem

        private DocumentMenuItem(final String fileName) throws Exception {
            this.fileName = fileName;
            caption = new File(fileName).getName(); // The stub for unopened documents.

            xml = (Element) storeDocument.getDocumentElement().appendChild(storeDocument.createElement("document"));
            bookmarks = (Element) xml.appendChild(storeDocument.createElement("bookmarks"));

            bookItems.add(0, this);
        } // DocumentMenuItem

        public void onItemClick() {
            DocumentViewerActivity.document = this;
            mainActivity().openViewer();
        }

        private void saveToXML() {
            xml.setAttribute("file", fileName);
            xml.setAttribute("isTest", String.valueOf(isTest));
            xml.setAttribute("caption", caption);
            xml.setAttribute("currentPhrase", String.valueOf(currentPhrase));
        }

        private void remove() {
            xml.getParentNode().removeChild(xml);
        }
    } // DocumentMenuItem
}
