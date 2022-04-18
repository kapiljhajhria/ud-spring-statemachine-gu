package com.jhajhria.statemachine.config;

import com.jhajhria.statemachine.domain.PaymentEvent;
import com.jhajhria.statemachine.domain.PaymentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

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


    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
//        super.configure(transitions);

        //PS.NEW -> PE.PRE_AUTHORIZE -> PS.NEW  //PRE_AUTHORIZE event won't change the state, we will still be in new state, the output of this event will change the state
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZE)
                .and().withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                .and().withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED);

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

}