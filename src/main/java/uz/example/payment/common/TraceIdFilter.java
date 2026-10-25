package uz.example.payment.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal (
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put(TRACE_ID_KEY, traceId);

        try {
            log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            log.info("Completed request: {} {} -> status {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus());

            MDC.remove(TRACE_ID_KEY);
        }
    }
}
