package priv.captain.spi;

import java.util.List;

public interface Search {

    // JDK 9之后，修饰符默认是public和abstract
    List<String> searchDocuments(String keyword);
}
