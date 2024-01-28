package com.arjun.btclient.records;

import eu.fraho.libs.beencode.BList;

import java.math.BigInteger;
import java.util.List;

public record TorrentFile(String announce, String comment, String createdBy, BigInteger creationDate, Info info, byte [] infoHash, BList urlList) {
}
