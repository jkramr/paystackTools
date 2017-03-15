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



  private PaymentMergeService paymentMergeService;

  @Autowired
  SampleInputDataCLR(
          PaymentMergeService paymentMergeService
  ) {
    this.paymentMergeService = paymentMergeService;
  }

  @Override
  public void run(String... args)
          throws Exception {

    paymentMergeService.init();
  }
}
