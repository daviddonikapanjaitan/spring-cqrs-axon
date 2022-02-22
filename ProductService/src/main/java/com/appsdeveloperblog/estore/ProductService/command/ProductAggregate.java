package com.appsdeveloperblog.estore.ProductService.command;

import java.math.BigDecimal;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.appsdeveloperblog.estore.ProductService.core.events.ProductCreatedEvent;
import com.appsdeveloperblog.estore.core.commands.CancelProductReservationCommand;
import com.appsdeveloperblog.estore.core.commands.ReserveProductCommand;
import com.appsdeveloperblog.estore.core.events.ProductReservationCancelledEvent;
import com.appsdeveloperblog.estore.core.events.ProductReserveEvent;

@Aggregate
public class ProductAggregate {
	
	@AggregateIdentifier
	private String productId;
	private String title;
	private BigDecimal price;
	private Integer quantity;
	
	public ProductAggregate() {
		
	}
	
	@CommandHandler
	public ProductAggregate(CreateProductCommand createProductCommand) {
		// Validate Create Product Command
		
		if(createProductCommand.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Price cannot be less or equal than zero");
		}
		
		if(createProductCommand.getTitle() == null || 
				createProductCommand.getTitle().isBlank()) {
			throw new IllegalArgumentException("Title cannot be empty");
		}
		
		ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
		
		BeanUtils.copyProperties(createProductCommand, productCreatedEvent);
		
		AggregateLifecycle.apply(productCreatedEvent);
	}
	
	@CommandHandler
	public void handle(ReserveProductCommand reserverProductCommand) {
		
		if(quantity < reserverProductCommand.getQuantity()) {
			throw new IllegalArgumentException("Insufficient number of items in stock");
		}
		
		ProductReserveEvent productReserveEvent = ProductReserveEvent.builder()
				.orderId(reserverProductCommand.getOrderId())
				.productId(reserverProductCommand.getProductId())
				.quantity(reserverProductCommand.getQuantity())
				.userId(reserverProductCommand.getUserId())
				.build();
	
		AggregateLifecycle.apply(productReserveEvent);
	}
	
	@CommandHandler
	public void handle(CancelProductReservationCommand cancelProductReservationCommand) {
		
		ProductReservationCancelledEvent productReservationCancelledEvent = 
				ProductReservationCancelledEvent.builder()
				.orderId(cancelProductReservationCommand.getOrderId())
				.productId(cancelProductReservationCommand.getProductId())
				.quantity(cancelProductReservationCommand.getQuantity())
				.reason(cancelProductReservationCommand.getReason())
				.userId(cancelProductReservationCommand.getUserId())
				.build();
		
		AggregateLifecycle.apply(productReservationCancelledEvent);
	}
	
	@EventSourcingHandler
	public void on(ProductReservationCancelledEvent productReservationCancelledEvent) {
		this.quantity += productReservationCancelledEvent.getQuantity();
	}
	
	@EventSourcingHandler
	public void on(ProductCreatedEvent productCreatedEvent) {
		this.productId = productCreatedEvent.getProductId();
		this.price = productCreatedEvent.getPrice();
		this.title = productCreatedEvent.getTitle();
		this.quantity = productCreatedEvent.getQuantity();
	}
	
	@EventSourcingHandler
	public void on(ProductReserveEvent productReserveEvent) {
		this.quantity -= productReserveEvent.getQuantity();
	}
}
