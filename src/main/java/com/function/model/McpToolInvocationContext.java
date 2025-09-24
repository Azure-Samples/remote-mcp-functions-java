package com.function.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * MCP tool invocation context that can handle any type of arguments.
 * Uses JsonObject for maximum flexibility across different MCP tools.
 */
public class McpToolInvocationContext {
    private String name;
    private JsonObject arguments;
    
    @SerializedName("_meta")
    private JsonObject meta;

    public McpToolInvocationContext() {}

    public McpToolInvocationContext(String name, JsonObject arguments, JsonObject meta) {
        this.name = name;
        this.arguments = arguments;
        this.meta = meta;
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
     * Gets the entire _meta object.
     * You can extract specific fields using methods like:
     * - meta.get("progressToken").getAsInt()
     */
    public JsonObject getMeta() {
        return meta;
    }

    /**
     * Sets the _meta object.
     */
    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    /**
     * Convenience method to check if an argument exists.
     */
    public boolean hasArgument(String argumentName) {
        return arguments != null && arguments.has(argumentName);
    }

    @Override
    public String toString() {
        return "McpToolInvocationContext{" +
                "name='" + name + '\'' +
                ", arguments=" + arguments +
                ", meta=" + meta +
                '}';
    }
}
