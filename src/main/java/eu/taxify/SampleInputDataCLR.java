package eu.taxify;

import eu.taxify.model.SourcePaymentRow;
import eu.taxify.model.PaystackPaymentRow;
import eu.taxify.repository.PaymentRepository;
import eu.taxify.repository.PaystackPaymentRepository;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Component
class SampleInputDataCLR implements CommandLineRunner {

    @Value("${srcPaymentsFilePath:null}")
    private String srcPaymentsFilePath;

    @Value("${srcPaymentsFileName:payment_src.csv}")
    private String srcPaymentsFileName;

    @Value("${paystackPaymentsFilePath:null}")
    private String paystackPaymentsFilePath;

    @Value("${paystackPaymentsFileName:payment_paystack.csv}")
    private String paystackPaymentsFileName;

    private PaymentRepository paymentRepository;
    private PaystackPaymentRepository paystackPaymentRepository;
    private PaymentMergeService paymentMergeService;

    @Autowired
    SampleInputDataCLR(
            PaymentRepository paymentRepository,
            PaystackPaymentRepository paystackPaymentRepository,
            PaymentMergeService paymentMergeService
    ) {
        this.paymentRepository = paymentRepository;
        this.paystackPaymentRepository = paystackPaymentRepository;
        this.paymentMergeService = paymentMergeService;
    }

    @Override
    public void run(String... args) throws Exception {
        SimpleFileReader srcFileReader = readFile(srcPaymentsFilePath, srcPaymentsFileName);

        Arrays.stream(srcFileReader.readFile()
                .split("\n"))
                .forEach(row -> {
                    String[] fields = row.split(",");
                    paymentRepository.save(new SourcePaymentRow(
                            fields[0], // id
                            fields[1], // type
                            LocalDateTime.parse(fields[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // created: 2017-02-22 15:02:18
                            Double.valueOf(fields[5]), // amount
                            fields[6], // payment_id
                            fields[7], // user_id
                            fields[8], // payment_method_id
                            fields[9], // payment_method_type
                            fields[10], // state
                            fields[11], // email
                            Integer.valueOf(fields[12]), // is_auto_retry
                            fields[13], // previous_row_id
                            fields[14]  // paystack_user_id
                    ));
                });

        SimpleFileReader paystackFileReader = readFile(paystackPaymentsFilePath, paystackPaymentsFileName);

        Arrays.stream(paystackFileReader.readFile()
                .split("\n"))
                .forEach(row -> {
                    String[] fields = row.split(",");
                    paystackPaymentRepository.save(new PaystackPaymentRow(
                            fields[0], // reference
                            fields[1], // email
                            Double.valueOf(fields[2]), // amount
                            fields[3], // status
                            LocalDateTime.parse(fields[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) // transaction date: Feb 15, 2017 1:30:08 pm
                    ));
                });

        paymentMergeService.initMerge();
    }

    private SimpleFileReader readFile(String path, String fileName) {
        try {
            return new SimpleFileReader(new FileReader(new File(path)));
        } catch (FileNotFoundException ignored) {
        }

        return readResource(fileName);
    }

    private SimpleFileReader readResource(String fileName) {
        return new SimpleFileReader(
                new InputStreamReader(this
                        .getClass()
                        .getClassLoader()
                        .getResourceAsStream(fileName)));
    }
}
