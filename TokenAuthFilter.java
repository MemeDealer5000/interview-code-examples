import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TokenAuthFilter extends TokenAuthenticationFilter {
    private final AuditService auditService;

    private final ObjectMapper objectMapper;

    private final Environment environment;

    @Value("${auth.service.allowed-roles}")
    private List<String> allowedRoles;

    public TokenAuthFilter(
            @Value("${app.base-secured-path}") String baseSecuredPath,
            IUserDetailsService userDetailsService,
            AuthenticationManager authenticationManager, Environment environment, AuditService auditService, ObjectMapper objectMapper) {
        super(baseSecuredPath, userDetailsService, authenticationManager);
        this.environment = environment;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }


    @Override
    public void doFilter(ServletRequest requestBase, ServletResponse responseBase, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) requestBase;
        HttpServletResponse response = (HttpServletResponse) responseBase;
        if (!requiresAuthentication(request, response)) {
            chain.doFilter(request, response);
        } else {
            try {
                Authentication authentication = attemptAuthentication(request, response);
                this.getSuccessHandler().onAuthenticationSuccess(request, response, authentication);
                createAuditA2Log();
                this.attemptAuthorization((UserDetails) authentication.getPrincipal());
                chain.doFilter(request, response);
            } catch (AuthenticationException e) {
                createAuditA3LogAndResponse(response, e, "Ошибка аутентификации");
                unsuccessfulAuthentication(request, response, e);
            } catch (AuthorizationServiceException e) {
                createAuditA3LogAndResponse(response, e, "Ошибка авторизации");
            } catch (HttpClientErrorException.Forbidden e) {
                createAuditA3LogAndResponse(response, e, "Ошибка аутентификации");
            }
        }
    }

    private void createAuditA2Log() {
        AuditAttributes auditAttributes = AuditAttributes.builder()
                .eventSubType(AuditEventType.SIGN_IN)
                .layer(LoggingLayer.CONTROLLER)
                .message("Пользователь успешно аутентифицирован")
                .status(LoggingStatus.SUCCESS)
                .build();
        auditService.audit(auditAttributes);
    }

    private void createAuditA3LogAndResponse(HttpServletResponse httpServletResponse,
                                             Exception e, String msg) throws IOException {

        String message = msg + " - " + e.getMessage();
        AuditAttributes auditAttributes = AuditAttributes.builder()
                .eventSubType(AuditEventType.FAIL_SIGN_IN)
                .reason(e.getMessage())
                .layer(LoggingLayer.CONTROLLER)
                .message(message)
                .status(LoggingStatus.FAIL)
                .build();
        auditService.audit(auditAttributes);
        ExceptionResponse exceptionResponse = new ExceptionResponse(message);
        httpServletResponse.setContentType("application/json");
        httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpServletResponse.getOutputStream().write(objectMapper.writeValueAsString(exceptionResponse).getBytes(StandardCharsets.UTF_8));
    }

    private void attemptAuthorization(PcapUserDetails userDetails) throws AuthorizationServiceException {
        if (this.environment.acceptsProfiles(Profiles.of("security-local"))) {
            return;
        }
        List<String> userRoles = userDetails.getUserInfo()
                .getRoles()
                .stream()
                .map(IPcapRole::getId)
                .collect(Collectors.toList());
        if (!CollectionUtils.containsAny(this.allowedRoles, userRoles)) {
            throw new AuthorizationServiceException(String.format("Пользователь %s не имеет необходимых ролей", userDetails.getUserLogin()));
        }
    }
}
