package com.edu.infnet.pb.payments.service;

import com.edu.infnet.pb.payments.dto.PaymentRequest;
import com.edu.infnet.pb.payments.dto.PaymentResponse;
import com.edu.infnet.pb.payments.entity.Payment;
import com.edu.infnet.pb.payments.enums.PaymentStatus;
import com.edu.infnet.pb.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentResponse create(PaymentRequest request, UUID userId) {
        var payment = Payment.builder()
                .userId(userId)
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .build();

        var saved = repository.save(payment);
        return toResponse(saved);
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

}
