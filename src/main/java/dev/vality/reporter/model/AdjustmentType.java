package dev.vality.reporter.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdjustmentType {
    CHANGE_AMOUNT("amount"),
    CHANGE_STATUS("status");

    private final String value;
}
