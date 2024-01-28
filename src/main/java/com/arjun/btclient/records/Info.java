package com.arjun.btclient.records;

import java.math.BigInteger;
import java.util.List;

public record Info(BigInteger length, byte [] name, BigInteger piecesLength, List<byte[]> piecesHash) {
}
