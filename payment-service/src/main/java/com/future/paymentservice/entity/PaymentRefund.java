package com.future.paymentservice.entity;

import com.future.futurecommon.constant.BankCardType;
import com.future.futurecommon.constant.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefund {
    @Id
    private Long id; // 退款ID（雪花算法生成）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment; // 关联支付表

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    private RefundStatus refundStatus = RefundStatus.REFUND_PENDING; // 退款状态

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount; // 退款金额

    @Column(name = "retries_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "refund_transaction_id", unique = true)
    private String refundTransactionId; // 第三方退款交易ID

    @Column(name = "user_first_name", nullable = false)
    private String userFirstName;

    @Column(name = "user_last_name", nullable = false)
    private String userLastName;

    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @Column(name = "name_on_card", nullable = false)
    private String nameOnCard;

    @Column(name = "security_code", nullable = false)
    private String securityCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_card_type", nullable = false)
    private BankCardType bankCardType; // 支付方式

    @Column(name = "reason")
    private String reason; // 退款原因

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 退款创建时间

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 退款更新时间
}
