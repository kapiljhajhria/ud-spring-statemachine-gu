package com.jhajhria.statemachine.config;

import com.jhajhria.statemachine.domain.PaymentEvent;
import com.jhajhria.statemachine.domain.PaymentState;
import com.jhajhria.statemachine.services.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;
import java.util.Random;

@Slf4j
@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
//        super.configure(states);
//    NEW, PRE_AUTH, PRE_AUTH_ERROR, AUTH, AUTH_ERROR
// https://docs.spring.io/spring-statemachine/docs/3.0.1/reference/#statemachine-examples-datajpa
        states.withStates()
                .initial(PaymentState.NEW)//initial state
                .states(EnumSet.allOf(PaymentState.class))//all possible states
                .end(PaymentState.AUTH)// now mention all the possible end states or terminal states
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
    }


    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
//        super.configure(transitions);

        //PS.NEW -> PE.PRE_AUTHORIZE -> PS.NEW  //PRE_AUTHORIZE event won't change the state, we will still be in new state, the output of this event will change the state
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZE).action(preAuthAction()).guard(paymentIdGuard())
                .and().withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                .and().withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED)
        //pre-auth to auth
                .and().withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH).event(PaymentEvent.AUTHORIZE).action(authAction())
                .and().withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH_ERROR).event(PaymentEvent.AUTH_DECLINED)
                .and().withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(PaymentEvent.AUTH_APPROVED);


    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
//        super.configure(model);
        //set up listeners
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<PaymentState, PaymentEvent>() {
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info(String.format("State changed from:%s, to:%s", from, to));
//                super.stateChanged(from, to);
            }
        };

        config.withConfiguration().listener(adapter);
    }


//- Guards help us ad an ability to state machine to prove an action
//- Ex in PaymentStateChangeInterceptor.preStateChange default id value has been se as -1L. If that ID is not found
//- Guards can enforce the ID requirement and if ID is null then actions will be disabled
//- Define paymentIdGuard
//- use paymentIdGuard in config
//- you can also chain multiple guards if required.
//- run repeated test to check if everything is still working
//- guard implemented for only one state change but you can do this for multiple state change as well.
    public Guard<PaymentState,PaymentEvent> paymentIdGuard(){
        return context -> {
            return context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER) != null;
        };
    }


    public Action<PaymentState, PaymentEvent> preAuthAction() {
        return context -> {
            log.info("Pre-authorizing the payment");

            if(new Random().nextInt(10)<=6){
                log.info("Pre-authorization successful");
//                context.getExtendedState().getVariables().put("preAuth", true);
                context.getStateMachine().sendEvent((MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build()));
            }else {
                log.info("Pre-authorization failed");
//                context.getExtendedState().getVariables().put("preAuth", false);
                context.getStateMachine().sendEvent((MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_DECLINED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build()));
            }


        };
    }

    public Action<PaymentState, PaymentEvent> authAction() {
        return context -> {
            log.info("Authorizing the payment");

            if(new Random().nextInt(10)<=4){
                log.info("Authorizing successful");
//                context.getExtendedState().getVariables().put("preAuth", true);
                context.getStateMachine().sendEvent((MessageBuilder.withPayload(PaymentEvent.AUTH_APPROVED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build()));
            }else {
                log.info("Authorizing failed");
//                context.getExtendedState().getVariables().put("preAuth", false);
                context.getStateMachine().sendEvent((MessageBuilder.withPayload(PaymentEvent.AUTH_DECLINED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build()));
            }


        };
    }

}