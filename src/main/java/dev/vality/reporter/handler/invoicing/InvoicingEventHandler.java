package dev.vality.reporter.handler.invoicing;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.reporter.handler.EventHandler;

public interface InvoicingEventHandler extends EventHandler<InvoiceChange> {
}
