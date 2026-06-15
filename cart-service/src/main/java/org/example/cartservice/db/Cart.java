package org.example.cartservice.db;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "carts",schema = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    public int getQuantity() {
        List<Integer> result = new ArrayList<>();
        cartItems.forEach(el -> {result.add(el.getQuantity());});
        return result.stream().reduce(0, Integer::sum);
    }


    public void clearCart() {
        this.cartItems.clear();
    }
}