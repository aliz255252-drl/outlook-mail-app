package com.example.outlookmail.service;

import com.example.outlookmail.dto.SendMailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for sending mail using Microsoft Graph API with app-only (client credentials) authentication.
 */
@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String senderMailbox;

    public GraphService(
            @Value("${graph.api.base-url}") String baseUrl,
            @Value("${azure.tenant-id}") String tenantId,
            @Value("${azure.client-id}") String clientId,
            @Value("${azure.client-secret}") String clientSecret,
            @Value("${azure.sender-mailbox}") String senderMailbox) {

        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.senderMailbox = senderMailbox;
    }

    /**
     * Obtains an app-only access token using the Client Credentials flow.
     */
    private String getAppAccessToken() {
        String tokenUrl = "https://login.microsoftonline.com/" + this.tenantId + "/oauth2/v2.0/token";
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", this.clientId);
        formData.add("client_secret", this.clientSecret);
        formData.add("scope", "https://graph.microsoft.com/.default");
        formData.add("grant_type", "client_credentials");

        RestClient tokenClient = RestClient.create();
        
        JsonNode response = tokenClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("access_token")) {
            return response.get("access_token").asText();
        }
        
        throw new RuntimeException("Failed to acquire app access token");
    }

    /**
     * Sends an email as the specified sender mailbox via POST /users/{senderMailbox}/sendMail.
     */
    public void sendMail(SendMailRequest request) {
        String accessToken = getAppAccessToken();
        
        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode message = payload.putObject("message");
        message.put("subject", request.getSubject());

        ObjectNode body = message.putObject("body");
        body.put("contentType", "Text");
        body.put("content", request.getBody());

        ArrayNode toRecipients = message.putArray("toRecipients");
        for (String address : splitAddresses(request.getTo())) {
            ObjectNode recipient = toRecipients.addObject();
            recipient.putObject("emailAddress").put("address", address);
        }

        payload.put("saveToSentItems", true);

        log.info("Sending mail from {} to {}...", senderMailbox, request.getTo());

        restClient.post()
                .uri("/users/" + senderMailbox + "/sendMail")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private List<String> splitAddresses(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (String part : raw.split(",|;")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
