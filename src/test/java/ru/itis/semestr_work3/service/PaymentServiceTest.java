package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.PaymentRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void findById_whenExists_returnsPayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThat(paymentService.findById(1L)).contains(payment);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(paymentService.findById(99L)).isEmpty();
    }

    @Test
    void findByBooking_whenExists_returnsPayment() {
        Payment payment = new Payment();
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(payment));

        assertThat(paymentService.findByBooking(10L)).contains(payment);
    }

    @Test
    void findByBooking_whenMissing_returnsEmpty() {
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        assertThat(paymentService.findByBooking(10L)).isEmpty();
    }

    @Test
    void pay_withValidMethod_setsStatusMethodAndPaidAt() {
        Payment payment = new Payment();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.pay(1L, "CARD");

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getMethod()).isEqualTo("CARD");
        assertThat(result.getPaidAt()).isNotNull();
    }

    @Test
    void pay_whenMethodIsNull_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> paymentService.pay(1L, null));
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    void pay_whenMethodIsBlank_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> paymentService.pay(1L, "   "));
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    void pay_whenPaymentMissing_throwsException() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.pay(1L, "CARD"));
    }

    @Test
    void pay_whenAlreadyPaid_throwsIllegalStateAndDoesNotSave() {
        Payment payment = new Payment();
        payment.setStatus("PAID");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.pay(1L, "CARD"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pay_whenRefunded_throwsIllegalStateAndDoesNotSave() {
        Payment payment = new Payment();
        payment.setStatus("REFUNDED");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalStateException.class, () -> paymentService.pay(1L, "CARD"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refund_whenPaymentExists_setsRefundedStatus() {
        Payment payment = new Payment();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.refund(1L);

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void refund_whenPaymentMissing_throwsException() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.refund(1L));
    }

    @Test
    void refund_whenAlreadyRefunded_isIdempotentAndDoesNotSave() {
        Payment payment = new Payment();
        payment.setStatus("REFUNDED");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.refund(1L);

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pay_whenUnsupportedMethod_throwsExceptionAndDoesNotTouchRepository() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.pay(1L, "BITCOIN")
        );
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    void pay_whenMethodInLowerCase_isNormalizedToUpperCase() {
        Payment payment = new Payment();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.pay(1L, "card");

        assertThat(result.getMethod()).isEqualTo("CARD");
    }
}