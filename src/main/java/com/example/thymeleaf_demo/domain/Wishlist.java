package com.example.thymeleaf_demo.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "wishlist", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","product_id"}))
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id",nullable = false)
    private Product product;

    public Wishlist() {
    }
}





















