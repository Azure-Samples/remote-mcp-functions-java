package com.function.model;

import com.google.gson.JsonObject;

/**
 * MCP tool invocation context that handles different transport scenarios.
 * Works with both HTTP streamable and SSE connections by using flexible JsonObject fields.
 */
public class McpToolInvocationContext {
    private String name;
    private JsonObject arguments;
    
    // Optional fields that may or may not be present depending on transport
    private String sessionid;           // Present in http-streamable
    private JsonObject clientinfo;      // Present in http-sse  
    private JsonObject transport;       // Present in both
    
    public McpToolInvocationContext() {}

    public McpToolInvocationContext(String name, JsonObject arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the arguments as a JsonObject for maximum flexibility.
     * You can extract specific fields using methods like:
     * - arguments.get("message").getAsString()
     * - arguments.get("snippetName").getAsString()
     */
    public JsonObject getArguments() {
        return arguments;
    }

    public void setArguments(JsonObject arguments) {
        this.arguments = arguments;
    }

    /**
     * Gets the session ID (present in http-streamable transport).
     */
    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    /**
     * Gets the client info object (present in http-sse transport).
     * You can extract fields like:
     * - clientinfo.get("name").getAsString()
     * - clientinfo.get("version").getAsString()
     */
    public JsonObject getClientinfo() {
        return clientinfo;
    }

    public void setClientinfo(JsonObject clientinfo) {
        this.clientinfo = clientinfo;
    }

    /**
     * Gets the transport information (present in both transports).
     * You can extract fields like:
     * - transport.get("name").getAsString()
     * - transport.get("properties").getAsJsonObject()
     */
    public JsonObject getTransport() {
        return transport;
    }

    public void setTransport(JsonObject transport) {
        this.transport = transport;
    }

    /**
     * Convenience method to check if an argument exists.
     */
    public boolean hasArgument(String argumentName) {
        return arguments != null && arguments.has(argumentName);
    }

    /**
     * Convenience method to get the transport type.
     */
    public String getTransportType() {
        return transport != null && transport.has("name") 
            ? transport.get("name").getAsString() 
            : null;
    }

    /**
     * Convenience method to check if this is HTTP streamable transport.
     */
    public boolean isHttpStreamable() {
        return "http-streamable".equals(getTransportType());
    }

    /**
     * Convenience method to check if this is HTTP SSE transport.
     */
    public boolean isHttpSse() {
        return "http-sse".equals(getTransportType());
    }

    @Override
    public String toString() {
        return "McpToolInvocationContext{" +
                "name='" + name + '\'' +
                ", arguments=" + arguments +
                ", sessionid='" + sessionid + '\'' +
                ", clientinfo=" + clientinfo +
                ", transport=" + transport +
                '}';
    }
}
