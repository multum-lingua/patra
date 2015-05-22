package com.poterin.patra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;

public class Utils {

    public static Context defaultContext;

    public static void log(String message) {
        Log.d("patra", message);
    }

    public static void logException(Exception e) {
        // todo think about exception log engine
        Log.e("patra", e.getMessage(), e);
    }

    public static void processException(Context context, Exception e) {
        processException(context, e, null);
    } // onException

    public static void processException(Context context, Exception e, DialogInterface.OnClickListener onOKClick) {
        logException(e);

        String message = e.getLocalizedMessage();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name);
        builder.setMessage(message);

        builder.setNeutralButton(android.R.string.ok, onOKClick);

        builder.show();
    } // onException

    private static String getBookInfo(Context context, Element element, String tagName, String defaultResult) {
        Element infoElement;

        try {
            infoElement = XMLUtil.findElement(element, tagName);
        }
        catch (XPathExpressionException e) {
            return defaultResult;
        }

        NodeList childs = infoElement.getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {
            if (
                childs.item(i).getNodeType() == Node.ELEMENT_NODE &&
                    childs.item(i).getNodeName().equals(Settings.primaryLanguage())
                )
            {
                return childs.item(i).getTextContent();
            }
        }

        return String.format(
            context.getString(R.string.undefined_phrase), Languages.getLanguageName(Settings.primaryLanguage()));
    }  // getBookInfo

    public static String getBookTitle(Context context, Element element) {
        return getBookInfo(context, element, "title", "NO TITLE") + " /" +
            getBookInfo(context, element, "author", "NO AUTHOR") + "/";
    }  // getBookTitle

    public static int ptToPixels(int pt) {
        DisplayMetrics metrics = defaultContext.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, pt, metrics));
    }
}
