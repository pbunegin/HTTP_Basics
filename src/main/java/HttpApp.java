import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpApp {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String CSRF_TOKEN = "\"csrfToken\":\"[^\"]++\"";
    private static final String YANDEX_UID = "yandexuid=[^\"]++";
    private static final String FILE_REQ_HEADERS = "src\\main\\java\\ReqHeaders";
    private HttpClient httpClient;
    private GetMethod method;
    private String url;

    public HttpApp(String url) {
        this.method = new GetMethod(url);
        this.url = url;
        this.httpClient = new HttpClient();
    }

    public void start() {
//        method.getParams().setParameter((HttpMethodParams.RETRY_HANDLER),
//                new DefaultHttpMethodRetryHandler(3, false));
        try {
            addRequestHeader();
            if (httpClient.executeMethod(method) != HttpStatus.SC_OK) {
                logger.info("Method failed: {}", method.getStatusLine());
            }
            String csrfToken = getCsrfToken();
            String yandexuid = getYandexUid();
            logger.info("{}  ::  {}", csrfToken, yandexuid);

            String newBody = executeRequest(csrfToken, yandexuid);
            logger.info(newBody);
        } catch (HttpException e) {
            logger.error("Fatal protocol violation: " + e.getMessage());
        } catch (IOException e) {
            logger.error("Fatal transport error: " + e.getMessage());
        } finally {
            // Release the connection.
            method.releaseConnection();
        }

    }

    private String executeRequest(String csrfToken, String yandexuid) throws IOException {
        method = new GetMethod(url);
        method.setQueryString("?text=%D0%B8%D0%B6%D0%B5%D0%B2%D1%81%D0%BA%2C%20%D0%BA%D0%BE%D0%BC%D0%BC%D1%83%D0%BD%D0%B0%D1%80%D0%BE%D0%B2%2C%20193&csrfToken="+csrfToken);
//        addRequestHeader();
        method.setRequestHeader("Cookie",yandexuid);
        if (httpClient.executeMethod(method) != HttpStatus.SC_OK) {
            logger.info("Method failed: {}", method.getStatusLine());
        }
        return method.getResponseBodyAsString();
    }

    private void addRequestHeader() throws IOException {
        List<String> strHeaders = null;
        strHeaders = Files.readAllLines(Paths.get(FILE_REQ_HEADERS));
        if (strHeaders != null) {
            for (String string : strHeaders) {
                method.addRequestHeader(string.split(": ")[0], string.split(": ")[1]);
            }
        }
    }

    private String getYandexUid() {
        Header header = method.getResponseHeader("Content-Security-Policy");
        return parseStr(header.getValue(),YANDEX_UID);
    }


    private String getCsrfToken() throws IOException {
        String result = method.getResponseBodyAsString();
        result = parseStr(result,CSRF_TOKEN);
        if (result != null){
            result = result.replaceAll("\\\"","").split(":")[1];
        }
        return result;
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