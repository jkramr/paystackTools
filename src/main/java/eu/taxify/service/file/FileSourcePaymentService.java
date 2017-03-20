package eu.taxify.service.file;

import eu.taxify.model.SourcePayment;
import eu.taxify.service.PaymentService;
import eu.taxify.util.FileSourceReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Consumer;

public class FileSourcePaymentService
        implements PaymentService<SourcePayment> {

  private final FileSourceReader fileSourceReader;

  @Value("${payment.file.path}")
  private String srcPaymentsFilePath;
  @Value("${payment.file.name:payment_src.csv}")
  private String srcPaymentsFileName;

  @Autowired
  public FileSourcePaymentService(FileSourceReader fileSourceReader) {
    this.fileSourceReader = fileSourceReader;
  }

  @Override
  public void consume(Consumer<SourcePayment> paymentConsumer) {
    fileSourceReader.readFile(
            srcPaymentsFilePath,
            srcPaymentsFileName,
            line -> paymentConsumer.accept(SourcePayment.parsePayment(line))
    );
  }
}
