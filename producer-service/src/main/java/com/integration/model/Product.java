package com.integration.model;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private Long productId;
    private String name;
    private int stock;
}
