package eu.taxify.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class CsvController {

  @RequestMapping(value = "/csv", method = RequestMethod.GET)
  public void csvExport(HttpServletResponse response)
          throws IOException {

    response.sendRedirect("/users");
  }
}