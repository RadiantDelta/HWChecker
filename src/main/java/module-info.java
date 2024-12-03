module com.example.hwcheckergui {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.logging;
    requires org.apache.commons.io;
    requires java.xml;
    requires docx4j;
    requires org.apache.commons.compress;
    requires junrar;
    requires java.datatransfer;
    requires java.desktop;
    requires org.apache.commons.lang3;
    requires org.tukaani.xz;

    opens com.example.hwcheckergui to javafx.fxml;
    exports com.example.hwcheckergui;
    exports com.example.hwcheckergui.util;
    opens com.example.hwcheckergui.util to javafx.fxml;
}