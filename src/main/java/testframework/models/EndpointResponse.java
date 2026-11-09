package testframework.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true, fluent = true)
public class EndpointResponse {

    @JsonProperty("result")
    private String result;

    @JsonProperty("message")
    private String message;
}