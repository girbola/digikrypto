module com.girbola.m2krypto {
  requires javafx.controls;
  requires javafx.fxml;


  opens com.girbola.m2krypto to javafx.fxml;
  exports com.girbola.m2krypto;
}

