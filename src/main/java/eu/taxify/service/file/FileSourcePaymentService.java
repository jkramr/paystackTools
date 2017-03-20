package eu.taxify.service.file;

import eu.taxify.model.SourcePayment;
import eu.taxify.service.PaymentService;
import eu.taxify.util.FileReaderWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Consumer;

import static eu.taxify.model.SourcePayment.parsePayment;

public class FileSourcePaymentService
        implements PaymentService<SourcePayment> {

  private final FileReaderWriter fileReaderWriter;

  @Value("${payments.file.quotes:false}")
  private boolean quotes;
  @Value("${payments.file.skip_first:false}")
  private boolean skipFirst;
  @Value("${payments.file.path:null}")
  private String  srcPaymentsFilePath;
  @Value("${payments.file.name:payment_src.csv}")
  private String  srcPaymentsFileName;

  public FileSourcePaymentService(FileReaderWriter fileSourceReader) {
    this.fileReaderWriter = fileSourceReader;
  }

  @Override
  public void consume(Consumer<SourcePayment> paymentConsumer) {
    fileReaderWriter.readFile(
            srcPaymentsFilePath,
            srcPaymentsFileName,
            lines -> paymentConsumer.accept(parsePayment(lines)),
            skipFirst
    );
  }
}
