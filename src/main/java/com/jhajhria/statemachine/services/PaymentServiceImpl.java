package com.jhajhria.statemachine.services;

import com.jhajhria.statemachine.domain.Payment;
import com.jhajhria.statemachine.domain.PaymentEvent;
import com.jhajhria.statemachine.domain.PaymentState;
import com.jhajhria.statemachine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final String PAYMENT_ID_HEADER = "payment_id";

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState,PaymentEvent> stateMachineFactory;

    private final PaymentStateChangeInterceptor paymentStateChangeInterceptor;
    @Transactional
    @Override
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW);

        return paymentRepository.save(payment);
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
        sendEvent(paymentId,stateMachine,PaymentEvent.PRE_AUTHORIZE);
        return stateMachine;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
        sendEvent(paymentId,stateMachine,PaymentEvent.AUTHORIZE);
        return stateMachine;
    }

    @Deprecated//using actions to progress the state machine
    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
        sendEvent(paymentId,stateMachine,PaymentEvent.AUTH_DECLINED);
        return stateMachine;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState,PaymentEvent> sm, PaymentEvent event) {
            Message msg = MessageBuilder.withPayload(event)
                             .setHeader(PAYMENT_ID_HEADER, paymentId)
                                                 .build();
        //Here we initial the state with certain tasks like  starting payment authorization process. in actions, we update state based on the response we get from API.
        // Ex if payment gets declines due to insufficient funds, actions will automatically update the state to DECLINE.
        // another way to look at this is that here we send the event and in actions it happens automatically (based on response or some other logic)

            sm.sendEvent(msg);

    }

    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
       //get payment from db using the id
        Payment payment = paymentRepository.findById(paymentId).get();
        //create a new state machine
        StateMachine<PaymentState, PaymentEvent> stateMachine = stateMachineFactory.getStateMachine(payment.getId().toString());

        stateMachine.stop();

        stateMachine.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(paymentStateChangeInterceptor);
            sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(),null,null,null));//set up context for the state machine.
            // Basically we are telling the state machine to stop, and we are setting state to match the payment data which we got from the db
        });

        //start the state machine
        stateMachine.start();

        return stateMachine;
    }
}