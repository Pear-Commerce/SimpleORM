package com.ericdmartell.maga.id;


import java.util.Date;
import java.util.UUID;


/**
 * 53 bit random long
 */
public class RandomIDGen extends IDGen {

    @Override
    public long getNext() {
        return (UUID.randomUUID().hashCode() & 0x1FFFFF) << 32 + UUID.randomUUID().hashCode();
    }
}
