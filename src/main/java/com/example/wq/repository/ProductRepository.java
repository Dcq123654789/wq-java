package com.example.wq.repository;

import com.example.wq.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 商品 Repository
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
}
