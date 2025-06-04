package ru.practicum.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.practicum.grpc.stats.UserActionControllerGrpc;
import ru.practicum.grpc.stats.UserActionProto;

@Component
@AllArgsConstructor
public class RequestInterceptor implements HandlerInterceptor {

    @GrpcClient("collector")
    private final UserActionControllerGrpc.UserActionControllerBlockingStub client;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        client.collectUserAction(UserActionProto.newBuilder()
                .setUserId())
        /*(new HitDto("ewm-main-service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now()));*/
        return true;
    }
}