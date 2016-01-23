package ru.nordmine.yandex.dic;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import java.io.IOException;

public class RequestHelper {

    private static final Logger logger = Logger.getLogger(RequestHelper.class);

    public static HttpResponse executeGetRequest(String url) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = client.execute(httpGet);
        } catch (IOException e) {
            logger.error(e);
        }
        return response;
    }
}
