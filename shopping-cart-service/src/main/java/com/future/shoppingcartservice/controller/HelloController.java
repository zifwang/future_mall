package com.future.shoppingcartservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shopping-cart-service")
public class HelloController {

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello() {
        return new ResponseEntity<>("Hello shopping cart service", HttpStatus.OK);
    }
}
