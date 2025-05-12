package com.sejong.drivinganalysis.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

/**
 * WebSocket 엔드포인트에 Spring 의존성 주입을 가능하게 하는 설정 클래스
 * WebSocket 엔드포인트(@ServerEndpoint)에서 @Autowired와 같은 Spring 기능을 사용할 수 있게 해준다.
 */
@Component
public class  CustomConfigurator extends ServerEndpointRegistration.Configurator implements ApplicationContextAware {

    // Spring의 BeanFactory를 저장하는 정적 변수 (모든 WebSocket 세션에서 공유)
    // volatile: 멀티스레드 환경에서 변수의 가시성 보장
    private static volatile BeanFactory context;

    /**
     * WebSocket 연결이 이루어질 때마다 호출되어 엔드포인트 인스턴스를 생성
     */
    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return context.getBean(clazz);
    }

    /**
     * Spring이 애플리케이션 시작 시 ApplicationContext를 주입
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CustomConfigurator.context = applicationContext;
    }
}