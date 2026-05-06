package com.example.trace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TraceUserNodeBindingUpdateRequest {

    /**
     * Complete replacement list for the user's operable trace nodes. Empty list
     * means clearing all node permissions for the user.
     */
    @NotNull(message = "nodeIds must not be null")
    @Size(max = 100, message = "nodeIds size must be <= 100")
    private List<@NotNull(message = "nodeId must not be null") Long> nodeIds;

    /**
     * Optional default node. If omitted and exactly one node is bound, that node
     * becomes the default automatically.
     */
    private Long defaultNodeId;
}
