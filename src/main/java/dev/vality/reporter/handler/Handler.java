package dev.vality.reporter.handler;

public interface Handler<T, R> {

    R handle(T value);

}
