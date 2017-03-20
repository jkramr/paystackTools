package eu.taxify.service.file;

import eu.taxify.model.PaystackPayment;
import eu.taxify.service.PaymentService;
import eu.taxify.util.FileSourceReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Consumer;

public class FilePaystackPaymentService
        implements PaymentService<PaystackPayment> {

  private final FileSourceReader fileSourceReader;

  @Value("${paystack.file.path}")
  private String paystackPaymentsFilePath;
  @Value("${paystack.file.name:payment_paystack.csv}")
  private String paystackPaymentsFileName;

  @Value("${paystack.file.fileCount:0}")
  private int fileCount;

  @Autowired
  public FilePaystackPaymentService(FileSourceReader fileSourceReader) {
    this.fileSourceReader = fileSourceReader;
  }

  @Override
  public void consume(Consumer<PaystackPayment> paymentConsumer) {
    fileSourceReader.readFile(
            paystackPaymentsFilePath,
            paystackPaymentsFileName,
            line -> paymentConsumer.accept(PaystackPayment.parseCSVPayment(
                    line))
    );

    for (int i = 1; i < fileCount; i++) {
      fileSourceReader.readFile(
              paystackPaymentsFilePath,
              paystackPaymentsFileName + "." + i,
              line -> paymentConsumer.accept(PaystackPayment.parseCSVPayment(
                      line))
      );
    }
  }

}
