package dev.vality.reporter.service;

import dev.vality.damsel.domain.Currency;

public interface DominantService {

    Currency getCurrency(String currencyCode);
}
