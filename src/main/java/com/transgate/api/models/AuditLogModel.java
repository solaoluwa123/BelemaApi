package com.transgate.api.models;

/**
 * Row from transgateweb_db.tbl_audit_log.
 */
public class AuditLogModel {

    private long id;
    private String event_time;
    private String actor_username;
    private String actor_email;
    private Integer actor_role;
    private String action;
    private String resource;
    private String http_method;
    private String request_path;
    private String ip_address;
    private String user_agent;
    private String outcome;
    private Integer http_status;
    private String details;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEvent_time() {
        return event_time;
    }

    public void setEvent_time(String event_time) {
        this.event_time = event_time;
    }

    public String getActor_username() {
        return actor_username;
    }

    public void setActor_username(String actor_username) {
        this.actor_username = actor_username;
    }

    public String getActor_email() {
        return actor_email;
    }

    public void setActor_email(String actor_email) {
        this.actor_email = actor_email;
    }

    public Integer getActor_role() {
        return actor_role;
    }

    public void setActor_role(Integer actor_role) {
        this.actor_role = actor_role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getHttp_method() {
        return http_method;
    }

    public void setHttp_method(String http_method) {
        this.http_method = http_method;
    }

    public String getRequest_path() {
        return request_path;
    }

    public void setRequest_path(String request_path) {
        this.request_path = request_path;
    }

    public String getIp_address() {
        return ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getUser_agent() {
        return user_agent;
    }

    public void setUser_agent(String user_agent) {
        this.user_agent = user_agent;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Integer getHttp_status() {
        return http_status;
    }

    public void setHttp_status(Integer http_status) {
        this.http_status = http_status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
