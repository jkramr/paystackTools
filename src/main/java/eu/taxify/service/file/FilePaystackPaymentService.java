package eu.taxify.service.file;

import eu.taxify.model.PaystackPayment;
import eu.taxify.service.PaymentService;
import eu.taxify.util.FileReaderWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.function.Consumer;

import static eu.taxify.model.PaystackPayment.parsePayment;

public class FilePaystackPaymentService
        implements PaymentService<PaystackPayment> {

  private final FileReaderWriter fileReaderWriter;

  @Value("${paystack.file.path:null}")
  private String  filePath;
  @Value("${paystack.file.name:payment_paystack.csv}")
  private String  fileName;
  @Value("${paystack.file.date_pattern:yyyy-MM-dd HH:mm:ss}")
  private String  fileDatePattern;
  @Value("${paystack.file.fields:0,1,2,3,4}")
  private int[]   fileFields;
  @Value("${paystack.file.skip_first:false}")
  private boolean fileSkipFirst;

  @Value("${paystack.file.updates.prefix:payment_paystack.csv.}")
  private String   filesPrefix;
  @Value("${paystack.file.updates.date_pattern:MMM dd, yyyy h:mm:ss a}")
  private String   filesDatePattern;
  @Value("${paystack.file.updates.fields:0,2,3,-1,1}")
  private int[]    filesFields;
  @Value("${paystack.file.updates.suffices:_2017031020}")
  private String[] suffices;
  @Value("${paystack.file.updates.skip_first:false}")
  private boolean  skipFirst;

  public FilePaystackPaymentService(
          FileReaderWriter fileReaderWriter
  ) {
    this.fileReaderWriter = fileReaderWriter;
  }

  @Override
  public void consume(Consumer<PaystackPayment> paymentConsumer) {
    fileReaderWriter.readFile(
            filePath,
            fileName,
            line -> paymentConsumer.accept(parsePayment(
                    line,
                    fileFields,
                    null,
                    fileDatePattern
            )),
            fileSkipFirst
    );

    Arrays.stream(suffices).forEach((suffix) -> {
      readWeeklyUpdate(paymentConsumer, suffix, "failed");

      readWeeklyUpdate(paymentConsumer, suffix, "success");
    });
  }

  private void readWeeklyUpdate(
          Consumer<PaystackPayment> paymentConsumer,
          String suffix,
          String status
  ) {
    fileReaderWriter.readFile(
            null,
            filesPrefix + status + suffix,
            line -> paymentConsumer.accept(parsePayment(
                    line,
                    filesFields,
                    status,
                    filesDatePattern
            )),
            skipFirst
    );
  }
}
