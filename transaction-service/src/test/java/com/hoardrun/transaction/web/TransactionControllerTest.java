package com.hoardrun.transaction.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoardrun.common.audit.AuditClient;
import com.hoardrun.transaction.config.JwtConfig;
import com.hoardrun.transaction.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditClient auditClient;

    @Test
    @WithMockUser(authorities = "SCOPE_transactions:read")
    void statusShouldReturnOkForReadAuthority() throws Exception {
        mockMvc.perform(get("/api/tx/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void statusShouldReturnOkForUserRole() throws Exception {
        mockMvc.perform(get("/api/tx/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void statusShouldBeForbiddenForMissingAuthority() throws Exception {
        mockMvc.perform(get("/api/tx/status")
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitLowValueTransactionShouldBeAccepted() throws Exception {
        var request = new TransactionController.TxRequest(100.0);
        mockMvc.perform(post("/api/tx/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.amount").value(100.0));

        verify(auditClient).emit(eq("TX_ACCEPTED"), any(), eq("{\"amount\":100.0}"));
    }

    @Test
    void submitHighValueTransactionWithoutMfaShouldRequireMfa() throws Exception {
        var request = new TransactionController.TxRequest(2000.0);
        mockMvc.perform(post("/api/tx/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("mfa_required"));

        verify(auditClient).emit(eq("TX_MFA_REQUIRED"), any(), eq("{\"amount\":2000.0}"));
    }

    @Test
    void submitHighValueTransactionWithMfaShouldBeAccepted() throws Exception {
        var request = new TransactionController.TxRequest(2000.0);
        mockMvc.perform(post("/api/tx/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(jwt -> jwt.claim("mfa_verified", true)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.amount").value(2000.0));

        verify(auditClient).emit(eq("TX_ACCEPTED"), any(), eq("{\"amount\":2000.0}"));
    }

    @Test
    void submitTransactionWithNegativeAmountShouldFail() throws Exception {
        var request = new TransactionController.TxRequest(-100.0);
        mockMvc.perform(post("/api/tx/submit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}