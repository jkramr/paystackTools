package eu.taxify.service;

import eu.taxify.model.PaystackPayment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PaystackService {

  @Value("${paystack.src.file_path:null}")
  private String paystackPaymentsFilePath;
  @Value("${paystack.src.file_name:payment_paystack.csv}")
  private String paystackPaymentsFileName;

  public void read(Consumer<PaystackPayment> consumer) {

  }
}
