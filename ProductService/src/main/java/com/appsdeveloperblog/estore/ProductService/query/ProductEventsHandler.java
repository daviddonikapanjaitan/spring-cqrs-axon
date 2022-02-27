package com.appsdeveloperblog.estore.ProductService.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.appsdeveloperblog.estore.ProductService.core.data.ProductEntity;
import com.appsdeveloperblog.estore.ProductService.core.data.ProductsRepository;
import com.appsdeveloperblog.estore.ProductService.core.events.ProductCreatedEvent;
import com.appsdeveloperblog.estore.core.events.ProductReservationCancelledEvent;
import com.appsdeveloperblog.estore.core.events.ProductReservedEvent;

@Component
@ProcessingGroup("product-group")
public class ProductEventsHandler {
	
	private final ProductsRepository productsRepository;
	private static final Logger logger = LoggerFactory.getLogger(ProductEventsHandler.class);
	
	public ProductEventsHandler(ProductsRepository productsRepository) {
		this.productsRepository = productsRepository;
	}
	
	@ExceptionHandler(resultType = Exception.class)
	public void handle(Exception exception) throws Exception {
		// log error message
		throw exception;
	}
	
	@ExceptionHandler(resultType = IllegalArgumentException.class)
	public void handle(IllegalArgumentException exception) {
		// log error message
	}

	@EventHandler
	public void on(ProductCreatedEvent event) throws Exception {
	
		ProductEntity productEntity = new ProductEntity();
		BeanUtils.copyProperties(event, productEntity);
		
		try {
			productsRepository.save(productEntity);
		}catch(IllegalArgumentException ex) {
			ex.printStackTrace();
		}
	}
	
	@EventHandler
	public void on(ProductReservedEvent productReserveEvent) {
		ProductEntity productEntity = productsRepository.findByProductId(productReserveEvent.getProductId());
		
		logger.debug("ProductReservedEvent: Current product quantity: " + productEntity.getQuantity());
		
		productEntity.setQuantity(productEntity.getQuantity() - productReserveEvent.getQuantity());
		
		productsRepository.save(productEntity);
		
		logger.debug("ProductReservedEvent: New product quantity: " + productEntity.getQuantity());
		
		logger.info("ProductReservedEvent is called for productId: " + productReserveEvent.getProductId() + " and orderId: " + productReserveEvent.getOrderId());
	}
	
	@EventHandler
	public void on(ProductReservationCancelledEvent productReservationCancelledEvent) {
		ProductEntity currentlyStoredProduct = productsRepository.findByProductId(productReservationCancelledEvent.getProductId());
		
		logger.debug("ProductReservationCancelledEvent: Current Product Quantity " + currentlyStoredProduct.getQuantity());
		
		int newQuantity = currentlyStoredProduct.getQuantity() + productReservationCancelledEvent.getQuantity();
		currentlyStoredProduct.setQuantity(newQuantity);
		
		productsRepository.save(currentlyStoredProduct);
		
		logger.debug("ProductReservationCancelledEvent: New Product Quantity " + currentlyStoredProduct.getQuantity());
	}
	
	@ResetHandler
	public void reset() {
		productsRepository.deleteAll();
	}
}
