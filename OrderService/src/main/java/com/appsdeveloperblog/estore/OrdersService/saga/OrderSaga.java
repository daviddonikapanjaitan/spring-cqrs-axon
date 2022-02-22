package com.appsdeveloperblog.estore.OrdersService.saga;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.appsdeveloperblog.estore.OrdersService.command.commands.ApproveOrderCommand;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderApprovedEvent;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderCreatedEvent;
import com.appsdeveloperblog.estore.core.commands.CancelProductReservationCommand;
import com.appsdeveloperblog.estore.core.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.estore.core.commands.ReserveProductCommand;
import com.appsdeveloperblog.estore.core.events.PaymentProcessedEvent;
import com.appsdeveloperblog.estore.core.events.ProductReservationCancelledEvent;
import com.appsdeveloperblog.estore.core.events.ProductReserveEvent;
import com.appsdeveloperblog.estore.core.model.User;
import com.appsdeveloperblog.estore.core.query.FetchUserPaymentDetailsQuery;

@Saga
public class OrderSaga {
	
	@Autowired
	private transient CommandGateway commandGateway;
	
	@Autowired
	private transient QueryGateway queryGateway;
	
	private static final Logger logger = LoggerFactory.getLogger(OrderSaga.class);

	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderCreatedEvent orderCreatedEvent) {
		
		ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
				.orderId(orderCreatedEvent.getOrderId())
				.productId(orderCreatedEvent.getProductId())
				.quantity(orderCreatedEvent.getQuantity())
				.userId(orderCreatedEvent.getUserId())
				.build();
		
		logger.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() + 
				" and productId: " + reserveProductCommand.getProductId());
		
		commandGateway.send(reserveProductCommand, new CommandCallback<ReserveProductCommand, Object>() {

			@Override
			public void onResult(CommandMessage<? extends ReserveProductCommand> commandMessage,
					CommandResultMessage<? extends Object> commandResultMessage) {
				   if(commandResultMessage.isExceptional()) {
					   // Start a compensating transaction
					   logger.info("Error command", commandResultMessage.getPayload());
				   }
			}
		});
	}
	
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(ProductReserveEvent productReserveEvent) {
		// Process user payment
		logger.info("ProductReservedEvent is called for productId: " + productReserveEvent.getProductId() + " and orderId: " + productReserveEvent.getOrderId());
	
		FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery = 
				new FetchUserPaymentDetailsQuery(productReserveEvent.getUserId());
		
		User userPaymentDetails = null; 
		
		try {
			userPaymentDetails = queryGateway.query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class)).join();
		}catch(Exception e) {
			logger.info(e.getMessage());
			
			// start compensating transaction
			cancelProductReservation(productReserveEvent, e.getMessage());
			return;
		}
		
		if(userPaymentDetails == null) {
			// Start compensating transaction
			cancelProductReservation(productReserveEvent, "Could not fetch user payment details");
			return;
		}
		
		logger.info("Successfully fetched user payment defaitls for user " + userPaymentDetails.getFirstName());
		
		ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
				.orderId(productReserveEvent.getOrderId())
				.paymentDetails(userPaymentDetails.getPaymentDetails())
				.paymentId(UUID.randomUUID().toString())
				.build();
		
		String result = null;
		try {
			commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
		}catch(Exception ex) {
			logger.error(ex.getMessage());
			// Start compensating transaction
			cancelProductReservation(productReserveEvent, ex.getMessage());
			return;
		}
		
		if (result == null) {
			logger.info("The ProcessPaymentCommand resulted in NULL. Initiating a compensating transaction");
			// Start compensating transaction
			cancelProductReservation(productReserveEvent, "Could not process user payment with provided payment details");
		}
	}
	
	private void cancelProductReservation(ProductReserveEvent productReserveEvent, String reason) {
		
		CancelProductReservationCommand publiCancelProductReservationCommand = 
				CancelProductReservationCommand.builder()
				.orderId(productReserveEvent.getOrderId())
				.productId(productReserveEvent.getProductId())
				.quantity(productReserveEvent.getQuantity())
				.userId(productReserveEvent.getUserId())
				.reason(reason)
				.build();
		
		commandGateway.send(publiCancelProductReservationCommand);
	}
	
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(PaymentProcessedEvent paymentProcessedEvent) {
		// Send an ApproveOrderCommand
		ApproveOrderCommand approveOrderCommand = 
				new ApproveOrderCommand(paymentProcessedEvent.getOrderId());
		
		commandGateway.send(approveOrderCommand);
	}
	
	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderApprovedEvent orderApprovedEvent) {
		logger.info("Order is approved. Order Saga is complete for orderId: " + orderApprovedEvent.getOrderId());
		// SagaLifecycle.end();
	}
	
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(ProductReservationCancelledEvent productReservationCancelledEvent) {
		// Create and send a RejectOrderCommand
	}
}
