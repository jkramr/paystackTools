package eu.taxify.controller;

import eu.taxify.service.PaymentMergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@RestController
public class MergeController {

  private final PaymentMergeService paymentMergeService;

  @Autowired
  public MergeController(
          PaymentMergeService paymentMergeService
  ) {
    this.paymentMergeService = paymentMergeService;
  }

  @RequestMapping(value = "/run", method = RequestMethod.GET)
  public void merge(HttpServletResponse response)
          throws IOException {

    paymentMergeService.run(System.out::println);

    response.sendRedirect("/users");
  }
}
