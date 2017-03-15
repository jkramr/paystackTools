package eu.taxify.service;

import eu.taxify.model.SourcePayment;
import eu.taxify.util.SimpleFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PaymentService {

  final
  SimpleFileReader simpleFileReader;

  @Autowired
  public PaymentService(SimpleFileReader simpleFileReader) {
    this.simpleFileReader = simpleFileReader;
  }

  public void read(Consumer<SourcePayment> function) {

  }
}
