package eu.taxify;

import eu.taxify.model.PaystackPayment;
import eu.taxify.model.SourcePayment;
import eu.taxify.service.file.FilePaystackPaymentService;
import eu.taxify.service.file.FileSourcePaymentService;
import eu.taxify.service.PaymentMergeService;
import eu.taxify.service.PaymentService;
import eu.taxify.util.FileSourceReader;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

@SpringBootApplication
@PropertySource("classpath:mysql.properties")
public class PaymentToolsApplication {

  @Value("${payment.src:file}")
  private String paymentSrc;

  @Value("${paystack.src:file}")
  private String paystackSrc;

  public static void main(String[] args) {
    SpringApplication.run(PaymentToolsApplication.class, args);
  }

  @Bean
  PaymentService<PaystackPayment> paystackPaymentService(
          FileSourceReader fileSourceReader
  ) {
    switch (paystackSrc) {
      case "file":
        return new FilePaystackPaymentService(fileSourceReader);
      default:
        return paymentConsumer -> { /*do nothing*/ };
    }
  }

  @Bean
  PaymentService<SourcePayment> sourcePaymentService(
          FileSourceReader fileSourceReader
  ) {
    switch (paymentSrc) {
      case "file":
        return new FileSourcePaymentService(fileSourceReader);
      default:
        return paymentConsumer -> { /*do nothing*/ };
    }
  }

  @Bean
  @Scope("prototype")
  Logger logger(InjectionPoint ip) {
    return Logger.getLogger(ip.getDeclaredType().getName());
  }

  @Bean
  CommandLineRunner commandLineRunner(
          PaymentMergeService paymentMergeService
  ) {

    return args -> paymentMergeService.init();
  }
}
