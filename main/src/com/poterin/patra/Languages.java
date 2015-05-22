package com.poterin.patra;

import java.util.*;

public class Languages {

    public static ArrayList<Locale> locales;
    public static String[] names, langIds;

    private static Languages one;

    public static void init() {
        one = new Languages();
    }

    public Languages() {
        locales = new ArrayList<Locale>();
        Locale[] systemLocales = Locale.getAvailableLocales();

        for (Locale locale : systemLocales) {
            if (!hasInLocales(locale.getISO3Language())) locales.add(locale);
        }

        Collections.sort(locales, new Comparator<Locale>() {
            @Override
            public int compare(Locale locale1, Locale locale2) {
                return locale1.getDisplayLanguage().compareToIgnoreCase(locale2.getDisplayLanguage());
            }
        });

        names = new String[locales.size()];
        langIds = new String[locales.size()];
        for (int i = 0; i < locales.size(); i++) {
            names[i] = locales.get(i).getDisplayLanguage();
            langIds[i] = locales.get(i).getISO3Language();
        }
    } // Languages

    private boolean hasInLocales(String langId) {
        for (Locale locale : locales)
            if (locale.getISO3Language().equals(langId)) return true;

        return false;
    }

    public static Locale getLocaleById(String langId) {
        for (Locale locale : locales)
            if (locale.getISO3Language().equals(langId)) return locale;

        throw new RuntimeException("Unknown language id");
    }

    public static String getLanguageName(String langId) {
        return getLocaleById(langId).getDisplayLanguage();
    }

    public static String getLangIdByName(String langName) {
        for (Locale locale : locales)
            if (locale.getDisplayLanguage().equals(langName)) return locale.getISO3Language();

        throw new RuntimeException("Unknown language language name");
    }

} // Languages