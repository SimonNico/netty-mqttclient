package com.nmqtt.utils;

public class ChannelClosedException extends RuntimeException {
    public ChannelClosedException(){}

    public ChannelClosedException(String msg){super((msg));}

    public ChannelClosedException(String msg,Throwable throwable){super(msg,throwable);}

    public ChannelClosedException(Throwable throwable) {
        super(throwable);
    }

}
