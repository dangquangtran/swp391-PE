/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.swp391.backend.model.user;

import com.swp391.backend.model.cartProduct.CartProduct;
import com.swp391.backend.model.receiveinfo.ReceiveInfo;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @author Lenovo
 */
@Data
@Builder
@AllArgsConstructor
public class UserDTO {
    private String email;
    private String firstname;
    private String lastname;
    private String imageurl;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private int receiveInfoPage;
    private List<CartProduct> cartProducts;
}
