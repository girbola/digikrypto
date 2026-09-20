module com.girbola.m2krypto {
  requires javafx.controls;
  requires javafx.fxml;
  requires opencv;
  requires java.desktop;
  requires tess4j;


  opens com.girbola.m2krypto to javafx.fxml;
  exports com.girbola.m2krypto;
}

