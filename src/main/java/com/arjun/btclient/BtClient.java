package com.arjun.btclient;

import com.arjun.btclient.records.Info;
import com.arjun.btclient.records.TorrentFile;
import eu.fraho.libs.beencode.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BtClient {

    private static final String CLIENT_ID = "-AR";
    private static final String VERSION = "0001";
    private static final int RANDOM_PART_LENGTH = 13;

    public static void main(String[] args) throws IOException, URISyntaxException {
        TorrentFile torrentFile = readTorrentFile("./src/main/resources/debian-12.4.0-amd64-netinst.iso.torrent");
        var peerId = generatePeerId();
        String encodedInfoHash = urlEncode(torrentFile.infoHash());
        String encodedPeerId = urlEncode(peerId.getBytes());

        var output = sendRequest(torrentFile.announce(), "info_hash=" + encodedInfoHash + "&peer_id=" + encodedPeerId + "&port=" + 6881 + "&uploaded=0" + "&downloaded=0" + "&left=" + torrentFile.info().length() + "&compact=1");
        System.out.println(output);

    }


    public static TorrentFile readTorrentFile(String path) throws IOException {
        try (InputStream is = Files.newInputStream(new File(path).toPath())) {
            BDict dict = BDict.of(is);
            BDict info = dict.get("info");
            BString pieces = info.get("pieces");
            BInteger length = info.get("length");
            BInteger pieceLength = info.get("piece length");
            BString name = info.get("name");
            var pieceHash = getPieceHashes(pieces);
            Info infBlock = new Info(length.getValue(), name.getValue(), pieceLength.getValue(), pieceHash);
            BString comment = dict.get("comment");
            BString announce = dict.get("announce");
            BString createdBy = dict.get("created by");
            BInteger creationDate = dict.get("creation date");
            BList urlList = dict.get("url-list");
            var infoHash = generateInfoHash(info);
            TorrentFile torrentFile = new TorrentFile(new String(announce.getValue()), new String(comment.getValue()), new String(createdBy.getValue()), creationDate.getValue(), infBlock, infoHash, urlList);
            return torrentFile;
        }
    }

    public static byte[] generateInfoHash(BDict infoDict) throws IOException {
        return computeSHA1(NodeFactory.encode(infoDict));
    }

    public static byte[] computeSHA1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to find SHA-1 algorithm", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static List<byte[]> getPieceHashes(BString pieces) {
        byte[] parts = pieces.getValue();
        int i = 0;
        List<byte[]> pieceHashes = new ArrayList<>();
        byte[] sha1Hash = new byte[20];
        int internalCount = 0;
        while (i < parts.length) {
            if (i % 20 == 0 && i != 0) {
                internalCount = 0;
                pieceHashes.add(sha1Hash);
                sha1Hash = new byte[20];
                sha1Hash[internalCount] = parts[i];
                internalCount++;
            } else {
                sha1Hash[internalCount] = parts[i];
                internalCount++;
            }
            i++;
        }
        return pieceHashes;
    }

    public static String sendRequest(String targetUrl, String parameterString) {
        StringBuilder response = new StringBuilder();
        HttpURLConnection connection = null;

        try {
            URL url = new URI(targetUrl + "?" + parameterString).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Transmission/3.00");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "deflate, gzip, br, zstd");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            } else {
                response.append("Error: ").append(responseCode);
            }
        } catch (Exception e) {
            response.append("Exception: ").append(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }

    public static String generatePeerId() {
        SecureRandom random = new SecureRandom();
        StringBuilder peerId = new StringBuilder(20);

        peerId.append(CLIENT_ID);
        peerId.append(VERSION);

        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            int randomChar = random.nextInt(10);
            peerId.append(randomChar);
        }

        return peerId.toString();
    }

    public static String urlEncode(byte[] data) {
        StringBuilder result = new StringBuilder();

        for (byte b : data) {
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || b == '.' || b == '-' || b == '~' || b == '_') {
                result.append((char) b);
            } else {
                result.append(String.format("%%%02x", b));
            }
        }

        return result.toString();
    }

}
