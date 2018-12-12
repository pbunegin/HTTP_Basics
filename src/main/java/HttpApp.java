import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpApp {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String CSRF_TOKEN_REGEX = "\"csrfToken\":\"[^\"]++\"";
    private static final String YANDEX_UID_REGEX = "yandexuid=[^\"]++";
    private static final String FILE_REQ_HEADERS = "src\\main\\java\\ReqHeaders";
    private String url;
    private String requestAddress;
    private HttpClient httpClient;
    private GetMethod method;

    public HttpApp(String url, String requestAddress) {
        this.url = url;
        this.requestAddress = requestAddress;
        this.httpClient = new HttpClient();
        this.method = new GetMethod(url);
    }

    public void start() {
        httpClient.getParams().setParameter(HttpMethodParams.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        String bodyRequest = executeRequest(null, null);
        String csrfToken = getCsrfToken(bodyRequest);
        String yandexuid = getYandexUid();
        logger.info("csrfToken = {}, {}", csrfToken, yandexuid);
        String newBodyRequest = executeRequest(csrfToken, yandexuid);
        String coordinates = getCoordinates(newBodyRequest);
        logger.info("Координаты: {}", coordinates);
    }

    private String getCoordinates(String newBodyRequest) {
        Document html = Jsoup.parse(newBodyRequest);
        Elements elementsConfigVew = html.getElementsByClass("config-view");
        if (elementsConfigVew.size() == 0){
            return null;
        }
        String mapLocation = elementsConfigVew.get(0).data();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(mapLocation);
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
        JSONObject searchPreloadedResults = (JSONObject) (jsonObject != null ? jsonObject.get("searchPreloadedResults") : null);
        JSONArray items = (JSONArray) (searchPreloadedResults != null ? searchPreloadedResults.get("items") : null);
        JSONObject item = (JSONObject) (items != null ? items.toArray()[0] : null);
        JSONArray coordinates = (JSONArray) (item != null ? item.get("coordinates") : null);
        return coordinates != null ? coordinates.toString() : null;
    }

    private String executeRequest(String csrfToken, String yandexuid) {
        method = new GetMethod(url);
        String result = null;
        if (csrfToken != null && !csrfToken.isEmpty() && yandexuid != null && !yandexuid.isEmpty()) {
            try {
                method.setQueryString("?text=" + URLEncoder.encode(requestAddress, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
            method.setRequestHeader("Cookie", yandexuid);
        } else {
            addRequestHeader();
        }
        try {
            if (httpClient.executeMethod(method) != HttpStatus.SC_OK) {
                logger.info("Method failed: {}", method.getStatusLine());
            }
            result = getResponseBody();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            method.releaseConnection();
        }
        return result;
    }

    private String getResponseBody() {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(),"UTF-8"))) {
            String str;
            while ((str = reader.readLine()) != null) {
                result.append(str);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return result.toString();
    }

    private void addRequestHeader() {
        List<String> strHeaders = null;
        try {
            strHeaders = Files.readAllLines(Paths.get(FILE_REQ_HEADERS));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        if (strHeaders != null) {
            for (String string : strHeaders) {
                method.addRequestHeader(string.split(": ")[0], string.split(": ")[1]);
            }
        }
    }

    private String getYandexUid() {
        Header header = method.getResponseHeader("Content-Security-Policy");
        return header != null ? parseStr(header.getValue(), YANDEX_UID_REGEX) : null;
    }

    private String getCsrfToken(String bodyRequest) {
        bodyRequest = parseStr(bodyRequest, CSRF_TOKEN_REGEX);
        if (bodyRequest != null) {
            bodyRequest = bodyRequest.replaceAll("\\\"", "").split(":")[1];
        }
        return bodyRequest;
    }

    private String parseStr(String str, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        if (m.find()) {
            return m.group();
        }
        return null;
    }
}