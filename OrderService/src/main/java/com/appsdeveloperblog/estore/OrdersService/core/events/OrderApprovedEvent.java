package com.appsdeveloperblog.estore.OrdersService.core.events;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.appsdeveloperblog.estore.OrdersService.core.model.OrderStatus;
import com.appsdeveloperblog.estore.core.events.PaymentProcessedEvent;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
public class OrderApprovedEvent {

	@TargetAggregateIdentifier
	private final String orderId;
	private final OrderStatus orderStatus = OrderStatus.APPROVED;
}
