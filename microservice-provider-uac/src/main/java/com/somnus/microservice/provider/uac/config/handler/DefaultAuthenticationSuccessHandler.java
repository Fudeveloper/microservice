package com.somnus.microservice.provider.uac.config.handler;

import com.google.common.collect.ImmutableMap;
import com.somnus.microservice.commons.base.utils.JacksonUtil;
import com.somnus.microservice.commons.base.utils.JwksUtil;
import com.somnus.microservice.commons.base.wrapper.WrapMapper;
import com.somnus.microservice.provider.uac.config.service.SecurityUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * @author kevin.liu
 * @title: DefaultAuthenticationSuccessHandler
 * @projectName uac
 * @description: 自定义登录成功Handler
 * @date 2021/11/15 15:06
 */
@Slf4j
@Component
public class DefaultAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    /**
     * token 过期时间
     */
    @Value("${jwt.token.expired}")
    private long jwtTokenExpired;

    /**
     * 刷新token 时间
     */
    @Value("${jwt.token.refresh.expired}")
    private long jwtTokenRefreshExpired;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        return Mono.defer(() -> Mono.just(webFilterExchange.getExchange().getResponse()).flatMap(response -> {

            DataBufferFactory dataBufferFactory = response.bufferFactory();

            // 生成JWT token
            SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();

            Map<String, Object> claims = ImmutableMap.of(
                    "realname", userDetails.getRealname(),
                    "username", userDetails.getUsername(),
                    "roles", userDetails.getAuthorities());

            String token = JwksUtil.generateToken(claims, userDetails.getUsername(), jwtTokenExpired, ChronoUnit.HOURS);

            String refreshToken = JwksUtil.generateToken(claims, userDetails.getUsername(), jwtTokenRefreshExpired, ChronoUnit.HOURS);

            Map<String, Object> tokenMap = ImmutableMap.of("token", token, "refreshToken", refreshToken);

            DataBuffer dataBuffer = dataBufferFactory.wrap(JacksonUtil.toJson(WrapMapper.success(tokenMap)).getBytes());

            return response.writeWith(Mono.just(dataBuffer));
        }));
    }
}