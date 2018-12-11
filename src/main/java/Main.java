public class Main {
    private static final String URL = "https://yandex.ru/maps";
    private static final String REQUEST_ADDRESS = "ижевск, коммунаров, 193";

    public static void main(String[] args) {
        HttpApp httpApp = new HttpApp(URL,REQUEST_ADDRESS);
        httpApp.start();
    }
}