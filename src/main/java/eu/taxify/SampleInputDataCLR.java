package eu.taxify;

import eu.taxify.service.PaymentMergeService;
import eu.taxify.util.SimpleFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

@Component
class SampleInputDataCLR
        implements CommandLineRunner {

  @Value("${srcPaymentsFilePath:null}")
  private String              srcPaymentsFilePath;
  @Value("${srcPaymentsFileName:payment_src.csv}")
  private String              srcPaymentsFileName;
  @Value("${paystackPaymentsFilePath:null}")
  private String              paystackPaymentsFilePath;
  @Value("${paystackPaymentsFileName:payment_paystack.csv}")
  private String              paystackPaymentsFileName;
  private PaymentMergeService paymentMergeService;

  @Autowired
  SampleInputDataCLR(
          PaymentMergeService paymentMergeService
  ) {
    this.paymentMergeService = paymentMergeService;
  }

  @Override
  public
  void run(String... args)
          throws Exception {

    paymentMergeService.init(
            readFile(srcPaymentsFilePath, srcPaymentsFileName),
            readFile(paystackPaymentsFilePath, paystackPaymentsFileName)
    );

    paymentMergeService.run(System.out::println);
  }

  private
  SimpleFileReader readFile(String path, String fileName) {
    try {
      return new SimpleFileReader(new FileReader(new File(path)));
    } catch (FileNotFoundException ignored) {
    }

    return readResource(fileName);
  }

  private
  SimpleFileReader readResource(String fileName) {
    return new SimpleFileReader(
            new InputStreamReader(this
                                          .getClass()
                                          .getClassLoader()
                                          .getResourceAsStream(fileName)));
  }
}
