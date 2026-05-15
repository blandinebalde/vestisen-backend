
package com.vendit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActionLogDTO {
    private Long id;
    private Long userId;
    private String username;
    private String userRole;
    private String httpMethod;
    private String requestUri;
    private String resourceType;
    private Long resourceId;
    private String actionLabel;
    private String queryString;
    private Integer responseStatus;
    private Boolean success;
    private String clientIp;
    private String userAgent;
    private String errorMessage;
    private LocalDateTime createdAt;
}
