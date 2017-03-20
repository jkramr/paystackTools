package eu.taxify.service;

import java.util.function.Consumer;

public interface PaymentService<S> {
  void consume(Consumer<S> paymentConsumer);
}
