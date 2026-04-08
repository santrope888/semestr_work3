package ru.itis.semestr_work3.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "patronymic")
    private String patronymic;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "license_path")
    private String licensePath;

    @Column(name = "license_status")
    private String licenseStatus = "NOT_UPLOADED";

    @Column(name = "license_uploaded_at")
    private LocalDate licenseUploadedAt;

    @Column(name = "passport_path")
    private String passportPath;

    @Column(name = "passport_status")
    private String passportStatus = "NOT_UPLOADED";

    @Column(name = "passport_uploaded_at")
    private LocalDate passportUploadedAt;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}