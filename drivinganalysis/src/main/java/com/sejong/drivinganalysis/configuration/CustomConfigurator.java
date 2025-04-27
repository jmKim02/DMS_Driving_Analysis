package com.sejong.drivinganalysis.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

/**
 * WebSocket 엔드포인트에 Spring 의존성 주입을 가능하게 하는 설정 클래스
 */
@Component
public class CustomConfigurator extends ServerEndpointRegistration.Configurator implements ApplicationContextAware {

    private static volatile BeanFactory context;

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return context.getBean(clazz);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CustomConfigurator.context = applicationContext;
    }
}