package com.poterin.patra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.List;

public class UserDictionary {

    public static UserDictionary userDictionary;
    private final Document storeDocument;
    private final Element root;
    private EditText editTextPhrase, editTextTranslation;

    public UserDictionary() throws Exception {
        userDictionary = this;
        storeDocument = XMLUtil.parseString(Settings.preferences.getString("user_dictionary", "<user_dictionary/>"));
        root = storeDocument.getDocumentElement();
    }

    public NodeList items() {
        return storeDocument.getDocumentElement().getChildNodes();
    }

    private void addItem(String phrase, String translation) {
        Element item = (Element) root.appendChild(storeDocument.createElement("item"));
        item.setAttribute("phrase", phrase);
        item.setAttribute("translation", translation);

        save();
    }

    public void save() {
        try {
            Settings.settingsEditor.putString("user_dictionary", XMLUtil.domToString(storeDocument, false));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Settings.settingsEditor.commit();
        DocumentViewerActivity.updateCachedPages();
    }

    public Element getItem(int index) {
        return (Element) items().item(index);
    }

    public Element findItem(String phrase) {
        for (int i = 0; i < items().getLength(); i++) {
            if (getItem(i).getAttribute("phrase").equalsIgnoreCase(phrase)) return getItem(i);
        }

        return null;
    }

    public void deleteItems(List<Element> items) {
        for (Element item: items) root.removeChild(item);
        save();
    }

    public void resetStudyProgress(List<Element> items) {
        for (Element item: items) {
            item.removeAttribute("direct_progress");
            item.removeAttribute("reverse_progress");
        }
        save();
    }

    public void deleteAllItems() {
        for (int i = items().getLength() - 1; i >= 0; i--) root.removeChild(items().item(i));
        save();
    }

    private void showEditItemDialog(
        final Context context,
        final int titleId,
        final String phrase,
        final String translation,
        final DialogInterface.OnClickListener onOKClick)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.add_to_dictionary_dialog, null);
        builder.setView(dialogView);

        editTextPhrase = (EditText) dialogView.findViewById(R.id.editTextPhrase);
        editTextPhrase.setText(phrase);

        editTextTranslation = (EditText) dialogView.findViewById(R.id.editTextTranslation);
        editTextTranslation.setText(translation);

        builder.setPositiveButton(android.R.string.ok, onOKClick);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    } // showEditItemDialog

    public void showAddToDictionaryDialog(
        final Context context,
        final String phrase,
        final String translation,
        final View.OnClickListener onOKClick)
    {
        showEditItemDialog(
            context,
            R.string.add_to_dictionary,
            phrase,
            translation,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    addItem(editTextPhrase.getText().toString(), editTextTranslation.getText().toString());
                    onOKClick.onClick(null);
                }
            }
        );
    } // showAddToDictionaryDialog

    public void showEditItemDialog(
        final Context context,
        final Element item,
        final View.OnClickListener onOKClick)
    {
        showEditItemDialog(
            context,
            R.string.edit_phrase_word,
            item.getAttribute("phrase"),
            item.getAttribute("translation"),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    item.setAttribute("phrase", editTextPhrase.getText().toString());
                    item.setAttribute("translation", editTextTranslation.getText().toString());
                    save();
                    onOKClick.onClick(null);
                }
            }
        );
    } // showEditItemDialog

    public int directProgress(Element item) {
        if (item.hasAttribute("direct_progress"))
            return Integer.valueOf(item.getAttribute("direct_progress"));
        else
            return 0;
    }

    public int reverseProgress(Element item) {
        if (item.hasAttribute("reverse_progress"))
            return Integer.valueOf(item.getAttribute("reverse_progress"));
        else
            return 0;
    }

    public int studyProgress(Element item) {
        return (directProgress(item) + reverseProgress(item)) * 25;
    }
}
