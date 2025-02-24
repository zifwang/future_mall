package com.future.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.futurecommon.constant.OrderEventType;
import com.future.futurecommon.constant.OrderStatus;
import com.future.futurecommon.constant.PaymentStatus;
import com.future.futurecommon.payload.PaymentResultDTO;
import com.future.futurecommon.util.SnowflakeIdGenerator;
import com.future.orderservice.entity.Order;
import com.future.orderservice.entity.OrderEvent;
import com.future.orderservice.entity.OrderItem;
import com.future.orderservice.exception.OrderAPIException;
import com.future.orderservice.exception.ResourceNotFoundException;
import com.future.orderservice.repository.OrderEventRepository;
import com.future.orderservice.repository.OrderRepository;
import com.future.orderservice.util.OrderEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentResultListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${kafka.consumer-config.topic}")
    private String listenerTopic;

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public PaymentResultListener(OrderRepository orderRepository, OrderEventRepository orderEventRepository, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @KafkaListener(topics = "payment_results")
    @Transactional
    public void handlePaymentResult(
            PaymentResultDTO result,
            @Header(KafkaHeaders.ACKNOWLEDGMENT) Acknowledgment acknowledgment
    ) {
        try {
            // 幂等性检查：如果订单已处理，直接提交偏移量
            Order order = orderRepository.findById(result.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", result.getOrderId()));

            if (PaymentStatus.UNPAID.equals(order.getPaymentStatus())) {
                throw new OrderAPIException("Can't cancel shipped order",
                        HttpStatus.BAD_REQUEST,
                        String.format("User [%d] can't cancel to shipped Order [%d]", order.getUserId(), order.getId()));
            }

            if (!order.getPaymentStatus().equals(PaymentStatus.PENDING)) {
                acknowledgment.acknowledge();
                return;
            }

            // 更新订单状态
            order.setPaymentStatus(result.getPaymentStatus());
            orderRepository.save(order);

            // order event 处理
            Map<String, Object> eventDataMap = Map.of(
                    "userId", order.getUserId(),
                    "orderId", order.getId(),
                    "Status Message", "Payment Successes"
            );
            OrderEventType orderEventType = result.getPaymentStatus().equals(PaymentStatus.COMPLETED) ? OrderEventType.PAYMENT_SUCCESS : OrderEventType.PAYMENT_FAILED;
            OrderEvent orderEvent = OrderEventUtil.generateOrderEvent(order, eventDataMap, OrderStatus.CONFIRMED, OrderStatus.CONFIRMED, orderEventType);
            orderEvent.setId(snowflakeIdGenerator.generateId());
            orderEventRepository.save(orderEvent);

            // TODO: if order fail, call item service to recover item
            // 未来，用kafka来做
            if (result.getPaymentStatus().equals(PaymentStatus.FAILED)) {
                Map<Long, OrderItem> itemsMap = order.getOrderItems()
                        .stream()
                        .collect(Collectors.toMap(OrderItem::getProductId, item -> item));

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(itemsMap);

                // api-call
            }

            // 未来功能： 发货

            logger.info("Order status updated: {} -> payment status: {}, order status: {}", order.getId(), order.getPaymentStatus(), order.getStatus());
        } catch (Exception ex) {
            // 订单不存在或其他异常
            logger.error("Failed to process payment result: {}", result.getOrderId(), ex);
            // 未来功能，增加功能以处理该类订单
        } finally {
            // 手动提交偏移量
            acknowledgment.acknowledge();
        }
    }
}
