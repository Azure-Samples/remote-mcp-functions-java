package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.McpPromptArgument;
import com.microsoft.azure.functions.annotation.McpPromptTrigger;

/**
 * Demonstrates MCP Prompt functions that expose prompt templates to MCP clients.
 * Clients discover prompts via prompts/list and invoke them via prompts/get.
 * 
 * Return a plain string (auto-wrapped into a single user message by the host)
 * or a JSON-serialized GetPromptResult for multi-message / rich content responses.
 */
public class PromptExamples {

    /**
     * A code review prompt with multiple arguments (1 required, 1 optional).
     * Uses McpPromptArgument annotations to define arguments in a strongly-typed way.
     */
    @FunctionName("CodeReviewPrompt")
    public String codeReviewPrompt(
            @McpPromptTrigger(
                    name = "code_review",
                    description = "Generates a code review prompt for the given code snippet",
                    title = "Code Review")
            String context,
            @McpPromptArgument(
                    name = "code",
                    argumentName = "code",
                    description = "The code to review",
                    isRequired = true)
            String code,
            @McpPromptArgument(
                    name = "language",
                    argumentName = "language",
                    description = "The programming language")
            String language,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Generating code review prompt");

        String lang = (language != null && !language.isEmpty()) ? language : "unknown";
        String snippet = (code != null && !code.isEmpty()) ? code : "// no code provided";

        return "Please review the following " + lang + " code and suggest improvements:\n\n```"
                + lang + "\n" + snippet + "\n```";
    }

    /**
     * A summarize prompt with a single required argument and plain string return.
     * The host auto-wraps the returned string into a PromptMessage with role "user".
     */
    @FunctionName("SummarizePrompt")
    public String summarizePrompt(
            @McpPromptTrigger(
                    name = "summarize",
                    description = "Summarizes the provided text",
                    title = "Summarize Text")
            String context,
            @McpPromptArgument(
                    name = "text",
                    argumentName = "text",
                    description = "The text to summarize",
                    isRequired = true)
            String text,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Generating summarize prompt");

        String input = (text != null && !text.isEmpty()) ? text : "No text provided";
        return "Please provide a concise summary of the following text:\n\n" + input;
    }

    /**
     * A prompt with no arguments. Tests the edge case of a prompt
     * that takes no user input.
     */
    @FunctionName("NoArgsPrompt")
    public String noArgsPrompt(
            @McpPromptTrigger(
                    name = "no_args_prompt",
                    description = "A prompt that requires no arguments",
                    title = "No Arguments Prompt")
            String context,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Generating no-args prompt");
        return "This prompt requires no arguments. Please provide general guidance.";
    }

    /**
     * A prompt with inline promptArguments JSON instead of McpPromptArgument annotations.
     * This is the alternative approach — useful when arguments are static/known at compile time.
     */
    @FunctionName("InlineArgsPrompt")
    public String inlineArgsPrompt(
            @McpPromptTrigger(
                    name = "inline_args_prompt",
                    description = "A prompt with arguments defined via inline JSON",
                    title = "Inline Arguments Prompt",
                    promptArguments = "[{\"name\":\"topic\",\"description\":\"The topic to discuss\",\"required\":false},"
                            + "{\"name\":\"style\",\"description\":\"The writing style\",\"required\":false}]")
            String context,
            final ExecutionContext executionContext) {

        executionContext.getLogger().info("Generating inline-args prompt");
        return "Please write about the specified topic in the requested style.";
    }
}
