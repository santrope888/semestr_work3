package ru.itis.semestr_work3.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_id")
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String color;

    @Column(name = "price_per_day", nullable = false)
    private Integer pricePerDay;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private String transmission;

    @Column(nullable = false)
    private String engine;

    @Column(nullable = false)
    private String drive;

    @Column(name = "image_path")
    private String imagePath;

    @Column
    private String description;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "car")
    private List<Booking> bookings;

    @OneToMany(mappedBy = "car")
    private List<Review> reviews;

    @OneToMany(mappedBy = "car")
    private List<Favorite> favorites;


}