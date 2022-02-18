package com.appsdeveloperblog.estore.ProductService.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products") // http://localhost:8080/products
public class ProductsController {
	
	@Autowired
	private Environment env;

	@PostMapping
	public String createProduct() {
		return "HTTP Post Handled";
	}
	
	@GetMapping
	public String getProduct() {
		return "HTTP Get Handled " + env.getProperty("local.server.port");
	}
	
	@PutMapping
	public String updateProduct() {
		return "HTTP Put Handled";
	}
	
	@DeleteMapping
	public String deleteProduct() {
		return "HTTP Delete Handled";
	}
}
