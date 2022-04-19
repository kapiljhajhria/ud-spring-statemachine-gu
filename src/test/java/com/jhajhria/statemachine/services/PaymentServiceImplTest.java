package com.jhajhria.statemachine.services;

import com.jhajhria.statemachine.domain.Payment;
import com.jhajhria.statemachine.domain.PaymentEvent;
import com.jhajhria.statemachine.domain.PaymentState;
import com.jhajhria.statemachine.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;

import java.math.BigDecimal;

@SpringBootTest
class PaymentServiceImplTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .amount(new BigDecimal("12.99")).build();

    }

    @Test
    void testPreAuth() {
        Payment savedPayment = paymentService.newPayment(payment);

        paymentService.preAuth(savedPayment.getId());

        Payment preAuthPayment = paymentRepository.findById(savedPayment.getId()).isPresent() ? paymentRepository.findById(savedPayment.getId()).get() : null;

        System.out.println(preAuthPayment);


    }

    @Test
    @RepeatedTest(20)
    void testAuth() {
        Payment savedPayment = paymentService.newPayment(payment);

        StateMachine<PaymentState, PaymentEvent> preAuthStateMachine =  paymentService.preAuth(savedPayment.getId());

        if(preAuthStateMachine.getState().getId()==(PaymentState.PRE_AUTH)){
            System.out.println("PreAuth- will try to auth now");
            StateMachine<PaymentState, PaymentEvent> authStateMachine = paymentService.authorizePayment(savedPayment.getId());
            System.out.println("Result of Auth: " + authStateMachine.getState().getId());

        }else {
            System.out.println("PreAuth failed so can't try auth right now, run test again");
        }

    }
}