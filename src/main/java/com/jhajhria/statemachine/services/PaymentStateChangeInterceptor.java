package com.jhajhria.statemachine.services;

import com.jhajhria.statemachine.domain.Payment;
import com.jhajhria.statemachine.domain.PaymentEvent;
import com.jhajhria.statemachine.domain.PaymentState;
import com.jhajhria.statemachine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
public class PaymentStateChangeInterceptor extends StateMachineInterceptorAdapter<PaymentState, PaymentEvent> {

    private final PaymentRepository paymentRepository;

    @Override
    public void preStateChange(State<PaymentState, PaymentEvent> state, Message<PaymentEvent> message, Transition<PaymentState, PaymentEvent> transition, StateMachine<PaymentState, PaymentEvent> stateMachine, StateMachine<PaymentState, PaymentEvent> rootStateMachine) {
//        super.preStateChange(state, message, transition, stateMachine, rootStateMachine);
        // Check if the message is present, not null
        // if message is not null then check if the message has header with key "paymentId"
        // get payment from paymentRepository by paymentId
        // set the state of the payment to the state of the state machine
        // save the payment
        // this will persist the state change in the database everytime
        Optional.ofNullable(message).ifPresent(msg -> {
            Optional.ofNullable(Long.class.cast(msg.getHeaders().getOrDefault(PaymentServiceImpl.PAYMENT_ID_HEADER, -1L))).ifPresent(
                    paymentId -> {
                        Payment payment = paymentRepository.getById(paymentId);

                        payment.setState(state.getId());

                        paymentRepository.save(payment);

                    }
            );


        }
        );
    }
}