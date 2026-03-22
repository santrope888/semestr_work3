package ru.itis.semestr_work3.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String currency; // RUB, USD, EUR

    @Column(nullable = false)
    private String status; // PENDING, PAID, REFUNDED

    @Column
    private String method; // CARD, CASH

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
}