import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String URL = "https://yandex.ru/maps";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        HttpClient httpClient = new HttpClient();
        GetMethod method = new GetMethod(URL);
        method.addRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//        method.addRequestHeader("Accept-Encoding","gzip, deflate, br");
        method.addRequestHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        method.addRequestHeader("Connection", "keep-alive");
        method.addRequestHeader("Cookie", "aps_los=1; yandexuid=2750297521541584633; mda=0; yandex_gid=44; _ym_uid=1542621242216627954; my=YwA=; fuid01=5bf2883a45ef6232.tEVhgFfT5MWY5a6BLeeqybRLntYgwn1QJ6FVcLHzuIniGBnfxLYKRuT0pLg7V-qFIgaBagriwxWjifwQtycdLI2K5FPlbg_hIc0azpnIZsy9W6wNwhOwfcDqq1oBQCVp; i=NpmBD/yhM1IeCof3whaWHyWENXd5o9YBdsjwUKQi9rOE1O1QvQ6+i0dHMhpGulWNvH9EmZUYQfJC2sD0HJLkP07+GhY=; yabs-frequency=/4/0000000000000000/xrImSFWo8VZQTMtuCY7kLB1m-391xKsmSFWo8CjKi73uCa2CLR1m-3AWmjBYRlWo8FjKi73vCW02_5ImSFaoO07iLB1m-38W/; _ym_isad=1; _ym_wasSynced=%7B%22time%22%3A1544430712503%2C%22params%22%3A%7B%22eu%22%3A0%7D%2C%22bkParams%22%3A%7B%7D%7D; _ym_d=1544430713; yp=1856944633.yrts.1541584633#1856944633.yrtsi.1541584633#1545213241.ygu.1#1560200315.szm.1:1920x1080:1920x889#1547028570.shlos.1#1547028570.los.1#1547028570.losc.0#1574921426.dsws.5#1574921426.dswa.0#1574921426.dwsets.5#1544517259.ln_tp.01; ys=ymrefl.B8797F72A94B8B72#wprid.1544436575421005-1549105187618573156679334-man1-3609");
        method.addRequestHeader("Host", "yandex.ru");
        method.addRequestHeader("Upgrade-Insecure-Requests", "1");
        method.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");

//        method.getParams().setParameter((HttpMethodParams.RETRY_HANDLER),
//                new DefaultHttpMethodRetryHandler(3, false));

        try {
            // Execute the method.
            int statusCode = httpClient.executeMethod(method);


            if (statusCode != HttpStatus.SC_OK) {
                logger.info("Method failed: {}", method.getStatusLine());
            }

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            String csrfToken = getCsrfToken(method.getResponseBodyAsString());
            String yandexuid = getYandexuid(method.getResponseHeader("Content-Security-Policy"));
            logger.info(method.getRequestHeader("Cookie").getElements()[0].getValue());

        } catch (HttpException e) {
            logger.error("Fatal protocol violation: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Fatal transport error: " + e.getMessage());
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

    private static String getYandexuid(Header cookie) {
        Pattern p = Pattern.compile("yandexuid=[^\"]++");
        Matcher m = p.matcher(cookie.getValue());
        m.find();
        String result = m.group();
        System.out.println(result);
        return result;
    }

    private static String getCsrfToken(String response) {
        Pattern p = Pattern.compile("\"csrfToken\":\"[^\"]++\"");
        Matcher m = p.matcher(response);
        String result = null;
        if (m.find()) {
            result = m.group();
        }
        logger.info(result);
        return result;
    }
}