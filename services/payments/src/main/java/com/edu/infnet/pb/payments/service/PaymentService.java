package com.edu.infnet.pb.payments.service;

import com.edu.infnet.pb.payments.dto.PaymentRequest;
import com.edu.infnet.pb.payments.dto.PaymentResponse;
import com.edu.infnet.pb.payments.entity.Payment;
import com.edu.infnet.pb.payments.dto.PaymentEvent;
import com.edu.infnet.pb.payments.enums.PaymentStatus;
import com.edu.infnet.pb.payments.producer.PaymentProducer;
import com.edu.infnet.pb.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.edu.infnet.pb.payments.dto.OrderPaymentRequestedEvent;
import com.edu.infnet.pb.payments.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentProducer producer;

    public PaymentResponse create(PaymentRequest request, UUID userId) {
        var processed = persistAndProcess(userId, request.amount(), request.method(), null);
        return toResponse(processed);
    }

    public void createFromOrder(OrderPaymentRequestedEvent event) {
        if (repository.existsByOrderId(event.orderId())) {
            log.warn("Pagamento já processado para orderId {}, ignorando mensagem duplicada", event.orderId());
            return;
        }

        persistAndProcess(event.userId(), event.amount(), PaymentMethod.PIX, event.orderId());
    }

    private Payment persistAndProcess(UUID userId, BigDecimal amount, PaymentMethod method, UUID orderId) {
        var payment = Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .amount(amount)
                .method(method)
                .status(PaymentStatus.PENDING)
                .build();

        var saved = repository.save(payment);
        producer.publish(toEvent(saved));

        var processed = process(saved);
        producer.publish(toEvent(processed));

        return processed;
    }

    public PaymentResponse findById(UUID id, UUID userId) {
        var payment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        if (!payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return toResponse(payment);
    }

    public List<PaymentResponse> findByUser(UUID userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse cancel(UUID id, UUID userId) {
        var payment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        if (!payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Apenas pagamentos com status PENDING podem ser cancelados");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        return toResponse(repository.save(payment));
    }

    private Payment process(Payment payment) {
        var status = payment.getAmount().compareTo(new BigDecimal("1000.00")) < 0
                ? PaymentStatus.APPROVED
                : PaymentStatus.FAILED;
        payment.setStatus(status);
        return repository.save(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private PaymentEvent toEvent(Payment payment) {
        return new PaymentEvent(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getUpdatedAt()
        );
    }
}
