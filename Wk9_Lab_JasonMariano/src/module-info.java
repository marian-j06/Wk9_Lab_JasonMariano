module Wk9_Lab_JasonMariano {
    requires javafx.controls;
    requires java.sql;

    exports application;

    // javafx.graphics needs this to launch Main, and javafx.base needs it so the table can read Patient's getters
    opens application to javafx.graphics, javafx.base;
}