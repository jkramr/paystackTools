package eu.taxify;

import eu.taxify.model.AdminPaymentRow;
import eu.taxify.model.PaystackPaymentRow;
import eu.taxify.repository.AdminPaymentRepository;
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

    @Value("${adminPaymentsFilePath:null}")
    private String adminPaymentsFilePath;

    @Value("${adminPaymentsFileName:payment_admin.csv}")
    private String adminPaymentsFileName;

    @Value("${paystackPaymentsFilePath:null}")
    private String paystackPaymentsFilePath;

    @Value("${paystackPaymentsFileName:null}")
    private String paystackPaymentsFileName;

    private AdminPaymentRepository adminPaymentRepository;
    private PaystackPaymentRepository paystackPaymentRepository;
    private PaymentMergeService paymentMergeService;

    @Autowired
    SampleInputDataCLR(
            AdminPaymentRepository adminPaymentRepository,
            PaystackPaymentRepository paystackPaymentRepository,
            PaymentMergeService paymentMergeService
    ) {
        this.adminPaymentRepository = adminPaymentRepository;
        this.paystackPaymentRepository = paystackPaymentRepository;
        this.paymentMergeService = paymentMergeService;
    }

    @Override
    public void run(String... args) throws Exception {
        SimpleFileReader adminFileReader = readFile(adminPaymentsFilePath, adminPaymentsFileName);

        Arrays.stream(adminFileReader.readFile()
                .split("\n"))
                .forEach(row -> {
                    String[] fields = row.split(",");
                    adminPaymentRepository.save(new AdminPaymentRow(
                            fields[0], // id
                            fields[1], // type
                            fields[2], // token
                            fields[3], // token_data
                            LocalDateTime.parse(fields[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // created: 2017-02-22 15:02:18
                            Double.valueOf(fields[5]), // amount
                            fields[6], // payment_id
                            fields[7], // user_id
                            fields[8], // payment_method_id
                            fields[9], // payment_method_type
                            fields[10], // state
                            fields[11], // generated_email
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
