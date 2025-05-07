package com.sejong.drivinganalysis.configuration;

import com.sejong.drivinganalysis.controller.FrameWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@Slf4j
public class WebSocketConfig {

    /**
     * ServerEndpoint 어노테이션이 붙은 클래스들을 웹소켓 엔드포인트로 등록해주는 빈
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}