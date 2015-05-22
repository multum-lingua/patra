package com.poterin.patra;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

public class LibraryFilterDialog {

    private static BookLoaderActivity bookLoader;
    private static ArrayList<String> langNames;
    private static String primaryLanguage;
    private static String secondaryLanguage;
    private static StringEnum genre = new StringEnum("any", "child", "adult");
    private static StringEnum rMethod = new StringEnum("any", "parallel", "frank");

    public static void loadSettings() {
        // todo save settings
        primaryLanguage = Settings.primaryLanguage();
        secondaryLanguage = Settings.secondaryLanguage();
        genre.setValue("any");
        rMethod.setValue("any");
    }

    public static void open(final BookLoaderActivity bookLoader) {
        LibraryFilterDialog.bookLoader = bookLoader;

        AlertDialog.Builder builder = new AlertDialog.Builder(bookLoader);
        builder.setIcon(R.drawable.ic_filter);
        builder.setTitle(
            bookLoader.getString(R.string.filter) + " - " + bookLoader.getString(R.string.internet_library));

        View dialogView = bookLoader.getLayoutInflater().inflate(R.layout.library_filter, null);
        builder.setView(dialogView);

        fillLangNames();

        final Spinner spinnerPrimaryLanguage = (Spinner) dialogView.findViewById(R.id.spinnerPrimaryLanguage);
        spinnerPrimaryLanguage.setAdapter(new ArrayAdapter<String>(
            bookLoader, android.R.layout.simple_spinner_dropdown_item, langNames));
        spinnerPrimaryLanguage.setSelection(getLangIndexById(primaryLanguage));

        final Spinner spinnerSecondaryLanguage = (Spinner) dialogView.findViewById(R.id.spinnerSecondaryLanguage);
        spinnerSecondaryLanguage.setAdapter(new ArrayAdapter<String>(
            bookLoader, android.R.layout.simple_spinner_dropdown_item, langNames));
        spinnerSecondaryLanguage.setSelection(getLangIndexById(secondaryLanguage));

        final Spinner spinnerGenre = (Spinner) dialogView.findViewById(R.id.spinnerGenre);
        spinnerGenre.setSelection(genre.getIndex());

        final Spinner spinnerReadMethod = (Spinner) dialogView.findViewById(R.id.spinnerReadMethod);
        spinnerReadMethod.setSelection(rMethod.getIndex());

        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                primaryLanguage = Languages.getLangIdByName(
                    langNames.get(spinnerPrimaryLanguage.getSelectedItemPosition()));
                secondaryLanguage = Languages.getLangIdByName(
                    langNames.get(spinnerSecondaryLanguage.getSelectedItemPosition()));
                genre.setIndex(spinnerGenre.getSelectedItemPosition());
                rMethod.setIndex(spinnerReadMethod.getSelectedItemPosition());

                bookLoader.setFilteredCatalogue(fillBookCatalogue(bookLoader));
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.show();
    } // open

    public static BookCatalogue fillBookCatalogue(BookLoaderActivity bookLoader) {
        String catalogueLangId = Locale.getDefault().getISO3Language();

        if (!catalogueLangId.equals(primaryLanguage) && !catalogueLangId.equals(secondaryLanguage))
            catalogueLangId = primaryLanguage;

        BookCatalogue result = new BookCatalogue(catalogueLangId);

        NodeList childs = bookLoader.catalogue.getDocumentElement().getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {
            Element element = (Element) childs.item(i);

            String g = element.hasAttribute("genre") ? element.getAttribute("genre") : "adult";
            String rm = element.hasAttribute("rmethod") ? element.getAttribute("rmethod") : "parallel";
            ArrayList<String> bookLanguages = BookCatalogue.getBookLanguages(element);

            if (
                (bookLanguages.contains(primaryLanguage) && bookLanguages.contains(secondaryLanguage))
                &&
                (genre.getIndex() == 0 || genre.getValue().equals(g))
                &&
                (rMethod.getIndex() == 0 || rMethod.getValue().equals(rm))
            )
                result.addBook(element);
        } // for

        Collections.sort(result.authors, new Comparator<BookCatalogue.Author>() {
            @Override
            public int compare(BookCatalogue.Author author, BookCatalogue.Author author2) {
                return author.name().compareToIgnoreCase(author2.name());
            }
        });

        return result;
    } // fillBookCatalogue

    private static int getLangIndexById(String langId) {
        Locale locale = Languages.getLocaleById(langId);
        return langNames.indexOf(locale.getDisplayLanguage());
    }

    private static void fillLangNames() {
        langNames = new ArrayList<String>();
        NodeList childs = bookLoader.catalogue.getDocumentElement().getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {
            Element element = (Element) childs.item(i);
            NodeList titles = XMLUtil.findFirstNode(element, "title").getChildNodes();

            for (int j = 0; j < titles.getLength(); j++) {
                String curLang = Languages.getLanguageName(titles.item(j).getNodeName());
                if (!langNames.contains(curLang))
                    langNames.add(curLang);
            }
        } // for

        Collections.sort(langNames);
    } // fillLangNames

    private static class StringEnum {
        private int index;
        private String[] accessibleValues;

        private StringEnum(String... accessibleValues) {
            this.accessibleValues = accessibleValues;
        }

        private void setValue(String value) {
            int i = Arrays.binarySearch(accessibleValues, value);
            if (i < 0)
                throw new RuntimeException("Inaccessible value: " + value);

            index = i;
        }

        private void setIndex(int index) {
            if (index < 0 || index >= accessibleValues.length)
                throw new RuntimeException("Inaccessible index: " + index);

            this.index = index;
        }

        private String getValue() {
            return accessibleValues[index];
        }

        private int getIndex() {
            return index;
        }
    } // StringEnum
}
