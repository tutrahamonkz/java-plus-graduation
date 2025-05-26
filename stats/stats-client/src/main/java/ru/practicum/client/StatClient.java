package ru.practicum.client;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.client.exception.StatsServerUnavailable;
import ru.practicum.dto.HitDto;
import ru.practicum.dto.StatsDto;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class StatClient {
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    @Autowired
    public StatClient(@Value("${stat-server.name}") String serverId, DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create(makeURI(serverId));
    }

    public ResponseEntity<Void> hit(@Valid HitDto hitDto) { //Сохранение информации о том, что на uri конкретного сервиса был отправлен запрос пользователем с ip
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Сохранение статистики для {}", hitDto);
            return response;
        } catch (RestClientException e) {
            log.error("Ошибка выполнения запроса post сервером статистики для запроса {} : {}, трассировка:", hitDto, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<List<StatsDto>> getStats(String start, String end, List<String> uris, boolean unique) { // Получение статистики по посещениям.
        try {
            ResponseEntity<List<StatsDto>> response = restClient.get()
                    .uri(buildStatsUri(start, end, uris, unique))
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {
                    });
            log.info("Выполнен запрос GET с параметрами start={}, end={}, uris={}, unique={}:", start, end, uris, unique);
            return response;
        } catch (RestClientException e) {
            log.error("Ошибка выполнения запроса GET на сервер статистики с параметрами start={}, " +
                            "end={}, uris={}, unique={}: {}, трассировка:", start, end, uris, unique,
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    private String buildStatsUri(String start, String end, List<String> uris, Boolean unique) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.replace(" ", "T"))
                .queryParam("end", end.replace(" ", "T"))
                .queryParam("uris", uris)
                .queryParam("unique", unique);
        return uriBuilder.toUriString();
    }

    private ServiceInstance getInstance(String serviceId) {
        try {
            return discoveryClient
                    .getInstances(serviceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + serviceId,
                    exception
            );
        }
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(5000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    private String makeURI(String serviceId) {
        ServiceInstance instance = createRetryTemplate().execute(cxt -> getInstance(serviceId));
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }
}