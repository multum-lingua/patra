package com.poterin.patra;

import com.poterin.andorra.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class BookCatalogue {
    public final String langId;

    public class Author {
        public ArrayList<Element> books;

        public Author() {
            books = new ArrayList<Element>();
            authors.add(this);
        }

        public String name() {
            return getAuthorName(books.get(0));
        }

        private boolean hasBook(Element book) {
            for (Element element : books) {
                if (element == book) return true;
            }
            return false;
        }
    } // Author

    public ArrayList<Author> authors;

    public BookCatalogue(String langId) {
        this.langId = langId;
        authors = new ArrayList<Author>();
    }

    private String getAuthorName(Element book) {
        return XMLUtil.findFirstNode(XMLUtil.findFirstNode(book, "author"), langId).getTextContent();
    }

    public Author getAuthor(Element book) {
        String authorName = getAuthorName(book);
        for (Author author : authors) {
            if (author.name().equalsIgnoreCase(authorName)) return author;
        }
        return null;
    }

    public void addBook(Element book) {
        Author author = getAuthor(book);
        if (author == null) {
            author = new Author();
        }
        else {
            if (author.hasBook(book)) return;
        }
        author.books.add(book);
    } // addBook


    public static ArrayList<String> getBookLanguages(Element book) {
        ArrayList<String> result = new  ArrayList<String>();

        NodeList titles = XMLUtil.findFirstNode(book, "title").getChildNodes();

        for (int i = 0; i < titles.getLength(); i++) {
            result.add(titles.item(i).getNodeName());
        }

        return result;
    }
} // BookCatalogue
