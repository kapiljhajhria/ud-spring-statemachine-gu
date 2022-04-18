package com.jhajhria.statemachine.config;

import com.jhajhria.statemachine.domain.PaymentEvent;
import com.jhajhria.statemachine.domain.PaymentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;

import java.util.EnumSet;

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


}