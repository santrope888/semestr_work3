package ru.itis.semestr_work3.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.repository.PaymentRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {
    private PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Optional<Payment> update(Long id, Payment payment) {
        if (!paymentRepository.existsById(id)) {
            return Optional.empty();
        }
        payment.setId(id);
        Payment paymentUpdated = paymentRepository.save(payment);
        return Optional.of(paymentUpdated);
    }

    public boolean deleteById(Long id) {
        if (!paymentRepository.existsById(id)) {
            return false;
        }

        paymentRepository.deleteById(id);

        return true;
    }
}
