package com.example.domain;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Component
public class DemoAgePatch {

    @NotNull
    @Min(18)
    private Integer age;
}
